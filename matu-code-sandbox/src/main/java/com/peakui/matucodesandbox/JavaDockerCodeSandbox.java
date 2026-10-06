package com.peakui.matucodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.peakui.matucodesandbox.model.ExecuteCodeRequest;
import com.peakui.matucodesandbox.model.ExecuteCodeResponse;
import com.peakui.matucodesandbox.model.ExecuteMessage;
import com.peakui.matucodesandbox.model.JudgeCaseResult;
import com.peakui.matucodesandbox.model.JudgeStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class JavaDockerCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String CONTAINER_APP_DIR = "/app";
    private static final long COMPILE_TIME_OUT_MILLIS = 15000L;
    private static final long COMPILE_MEMORY_LIMIT = 1024L * 1024 * 1024;
    private static final int DEFAULT_MEMORY_LIMIT_MB = 256;
    private static final int DEFAULT_TIME_LIMIT_MILLIS = 1000;
    private static final long TMPFS_SIZE_MB = 64L;
    private static final String JAVA_DOCKER_IMAGE = "docker.1panel.live/library/openjdk:8-alpine";
    private static final ConcurrentHashMap<String, Boolean> IMAGE_INIT_MAP = new ConcurrentHashMap<>();

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        if (executeCodeRequest == null || StrUtil.isBlank(executeCodeRequest.getLanguage())) {
            throw new IllegalArgumentException("language 不能为空");
        }
        DockerLanguageConfig languageConfig = getLanguageConfig(executeCodeRequest.getLanguage());
        File userCodeFile = null;
        String containerId = null;
        DockerClient dockerClient = createDockerClient();
        try {
            List<String> inputList = executeCodeRequest.getInputList();
            if (inputList == null) {
                inputList = Collections.emptyList();
            }
            userCodeFile = saveCodeToFile(executeCodeRequest.getCode(), languageConfig.getSourceFileName());
            initImageIfNeeded(dockerClient, languageConfig);
            ExecuteMessage compileResult = compileInContainer(dockerClient, userCodeFile, languageConfig);
            if (compileResult.getExitValue() != null && compileResult.getExitValue() != 0) {
                return buildCompileErrorResponse(compileResult);
            }
            int memoryLimitMb = languageConfig.resolveMemoryLimitMb(executeCodeRequest.getMemoryLimit());
            long runTimeoutMillis = languageConfig.resolveTimeoutMillis(executeCodeRequest.getTimeLimit());
            containerId = createAndStartContainer(dockerClient, userCodeFile.getParentFile().getAbsolutePath(), languageConfig.getImage(), mbToBytes(memoryLimitMb));
            List<ExecuteMessage> executeMessageList = runInContainer(dockerClient, containerId, inputList, languageConfig, memoryLimitMb, runTimeoutMillis);
            return buildOutputResponse(executeMessageList, compileResult.getMessage());
        } catch (Exception e) {
            log.error("docker execute code error", e);
            return getSystemErrorResponse(e);
        } finally {
            if (containerId != null) {
                removeContainerQuietly(dockerClient, containerId);
            }
            if (userCodeFile != null) {
                deleteFile(userCodeFile);
            }
        }
    }

    private File saveCodeToFile(String code, String fileName) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + fileName;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    private DockerClient createDockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(java.time.Duration.ofSeconds(30))
                .responseTimeout(java.time.Duration.ofSeconds(30))
                .build();
        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(dockerHttpClient)
                .build();
    }

    private void initImageIfNeeded(DockerClient dockerClient, DockerLanguageConfig languageConfig) throws InterruptedException {
        String image = languageConfig.getImage();
        if (IMAGE_INIT_MAP.containsKey(image)) {
            return;
        }
        try {
            InspectImageResponse inspectImageResponse = dockerClient.inspectImageCmd(image).exec();
            if (inspectImageResponse != null) {
                IMAGE_INIT_MAP.put(image, true);
                log.info("image already exists locally: {}", image);
                return;
            }
        } catch (Exception e) {
            log.info("image not found locally: {}", image);
        }
        if (!languageConfig.allowPull()) {
            throw new IllegalStateException("本地未找到 Java 运行镜像，请先构建: " + image);
        }
        dockerClient.pullImageCmd(image)
                .exec(new PullImageResultCallback() {
                    @Override
                    public void onNext(PullResponseItem item) {
                        log.info("pull image {}: {}", image, item.getStatus());
                        super.onNext(item);
                    }
                })
                .awaitCompletion();
        IMAGE_INIT_MAP.put(image, true);
    }

    private ExecuteMessage compileInContainer(DockerClient dockerClient, File userCodeFile, DockerLanguageConfig languageConfig) throws InterruptedException {
        if (!languageConfig.needCompile()) {
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExitValue(0);
            executeMessage.setTime(0L);
            executeMessage.setTimeout(false);
            return executeMessage;
        }
        String containerId = createAndStartContainer(dockerClient, userCodeFile.getParentFile().getAbsolutePath(), languageConfig.getImage(), COMPILE_MEMORY_LIMIT);
        try {
            return execCommand(dockerClient, containerId, languageConfig.getCompileCommand(), "", false, COMPILE_TIME_OUT_MILLIS);
        } finally {
            removeContainerQuietly(dockerClient, containerId);
        }
    }

    private String createAndStartContainer(DockerClient dockerClient, String userCodeParentPath, String image, long memoryLimitBytes) {
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(memoryLimitBytes);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume(CONTAINER_APP_DIR)));
        hostConfig.withTmpFs(Collections.singletonMap("/tmp", "rw,size=" + TMPFS_SIZE_MB + "m"));
        CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(image)
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .withWorkingDir(CONTAINER_APP_DIR)
                .withCmd("sh", "-c", "while true; do sleep 1; done")
                .exec();
        String containerId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }

    private List<ExecuteMessage> runInContainer(DockerClient dockerClient, String containerId, List<String> inputList,
                                                DockerLanguageConfig languageConfig, int memoryLimitMb,
                                                long timeoutMillis) throws InterruptedException {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        List<String> actualInputList = inputList.isEmpty() ? Collections.singletonList("") : inputList;
        for (String inputArgs : actualInputList) {
            String[] command = languageConfig.buildRunCommand(memoryLimitMb);
            executeMessageList.add(execCommand(dockerClient, containerId, command, inputArgs, true, timeoutMillis));
        }
        return executeMessageList;
    }

    private ExecuteMessage execCommand(DockerClient dockerClient, String containerId, String[] command,
                                       String stdinInput, boolean collectMemory, long timeoutMillis) throws InterruptedException {
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(command)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();

        ExecuteMessage executeMessage = new ExecuteMessage();
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();
        final boolean[] timeout = {true};
        final long[] maxMemory = {0L};
        String execId = execCreateCmdResponse.getId();

        ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
            @Override
            public void onComplete() {
                timeout[0] = false;
                super.onComplete();
            }

            @Override
            public void onNext(Frame frame) {
                String payload = new String(frame.getPayload(), StandardCharsets.UTF_8);
                if (StreamType.STDERR.equals(frame.getStreamType())) {
                    stderrBuilder.append(payload);
                } else {
                    stdoutBuilder.append(payload);
                }
                super.onNext(frame);
            }
        };

        StatsCmd statsCmd = null;
        ResultCallback<Statistics> statisticsResultCallback = null;
        if (collectMemory) {
            statsCmd = dockerClient.statsCmd(containerId);
            statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onNext(Statistics statistics) {
                    if (statistics.getMemoryStats() != null && statistics.getMemoryStats().getUsage() != null) {
                        maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.debug("stats stream closed for container {}", containerId, throwable);
                }

                @Override
                public void onComplete() {
                }

                @Override
                public void close() throws IOException {
                }
            });
        }

        StopWatch stopWatch = new StopWatch();
        boolean completed;
        try {
            stopWatch.start();
            completed = dockerClient.execStartCmd(execId)
                    .withDetach(false)
                    .withTty(false)
                    .withStdIn(new ByteArrayInputStream(buildStdinInput(stdinInput).getBytes(StandardCharsets.UTF_8)))
                    .exec(execStartResultCallback)
                    .awaitCompletion(timeoutMillis, TimeUnit.MILLISECONDS);
            stopWatch.stop();
        } finally {
            if (statisticsResultCallback != null) {
                try {
                    statisticsResultCallback.close();
                } catch (IOException ignored) {
                }
            }
            if (statsCmd != null) {
                statsCmd.close();
            }
        }

        executeMessage.setTime(stopWatch.getTotalTimeMillis());
        executeMessage.setMemory(maxMemory[0] / 1024);
        executeMessage.setTimeout(!completed || timeout[0]);
        executeMessage.setMessage(normalizeOutput(stdoutBuilder.toString()));
        executeMessage.setErrorMessage(normalizeOutput(stderrBuilder.toString()));
        if (!completed || timeout[0]) {
            executeMessage.setExitValue(-1);
            if (StrUtil.isBlank(executeMessage.getErrorMessage())) {
                executeMessage.setErrorMessage("运行超时");
            }
            return executeMessage;
        }

        Integer exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCode();
        executeMessage.setExitValue(exitCode == null ? 0 : exitCode);
        return executeMessage;
    }

    private String normalizeOutput(String output) {
        if (output == null) {
            return null;
        }
        return StrUtil.removeSuffix(output, "\n");
    }

    private String buildStdinInput(String stdinInput) {
        if (stdinInput == null) {
            return "";
        }
        return stdinInput.endsWith("\n") ? stdinInput : stdinInput + "\n";
    }

    private void removeContainerQuietly(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            log.warn("remove container error, containerId={}", containerId, e);
        }
    }

    private boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            return FileUtil.del(userCodeFile.getParentFile().getAbsolutePath());
        }
        return true;
    }

    private ExecuteCodeResponse buildCompileErrorResponse(ExecuteMessage compileResult) {
        String compileError = StrUtil.isNotBlank(compileResult.getErrorMessage())
                ? compileResult.getErrorMessage()
                : compileResult.getMessage();
        return ExecuteCodeResponse.builder()
                .outputList(new ArrayList<>())
                .caseResults(new ArrayList<>())
                .status(JudgeStatusEnum.COMPILE_ERROR.getValue())
                .executionTime(compileResult.getTime() == null ? 0L : compileResult.getTime())
                .memoryUsed(0L)
                .errorMessage(compileError)
                .compileMessage(compileError)
                .build();
    }

    private ExecuteCodeResponse getSystemErrorResponse(Throwable e) {
        return ExecuteCodeResponse.builder()
                .outputList(new ArrayList<>())
                .caseResults(new ArrayList<>())
                .status(JudgeStatusEnum.SYSTEM_ERROR.getValue())
                .executionTime(0L)
                .memoryUsed(0L)
                .errorMessage(e.getMessage())
                .compileMessage(null)
                .build();
    }

    private ExecuteCodeResponse buildOutputResponse(List<ExecuteMessage> executeMessageList, String compileMessage) {
        List<String> outputList = new ArrayList<>();
        List<JudgeCaseResult> caseResults = new ArrayList<>();
        long maxTime = 0L;
        long maxMemory = 0L;
        int finalStatus = JudgeStatusEnum.ACCEPTED.getValue();
        String finalErrorMessage = null;

        for (int i = 0; i < executeMessageList.size(); i++) {
            ExecuteMessage executeMessage = executeMessageList.get(i);
            String stdout = executeMessage.getMessage();
            String stderr = executeMessage.getErrorMessage();
            Long executionTime = executeMessage.getTime() == null ? 0L : executeMessage.getTime();
            Long memoryUsed = executeMessage.getMemory() == null ? 0L : executeMessage.getMemory();
            Integer exitCode = executeMessage.getExitValue() == null ? -1 : executeMessage.getExitValue();

            int caseStatus = JudgeStatusEnum.ACCEPTED.getValue();
            String errorMessage = null;
            if (Boolean.TRUE.equals(executeMessage.getTimeout())) {
                caseStatus = JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getValue();
                errorMessage = "超出时间限制";
            } else if (exitCode == 137) {
                caseStatus = JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED.getValue();
                errorMessage = "超出内存限制";
            } else if (StrUtil.isNotBlank(stderr) || exitCode != 0) {
                caseStatus = JudgeStatusEnum.RUNTIME_ERROR.getValue();
                errorMessage = StrUtil.isNotBlank(stderr) ? stderr : "程序运行失败";
            }

            if (caseStatus != JudgeStatusEnum.ACCEPTED.getValue() && finalStatus == JudgeStatusEnum.ACCEPTED.getValue()) {
                finalStatus = caseStatus;
                finalErrorMessage = errorMessage;
            }

            outputList.add(stdout);
            caseResults.add(JudgeCaseResult.builder()
                    .caseNo(i + 1)
                    .status(caseStatus)
                    .output(stdout)
                    .executionTime(executionTime)
                    .memoryUsed(memoryUsed)
                    .exitCode(exitCode)
                    .errorMessage(errorMessage)
                    .stderr(stderr)
                    .build());
            maxTime = Math.max(maxTime, executionTime);
            maxMemory = Math.max(maxMemory, memoryUsed);
        }

        return ExecuteCodeResponse.builder()
                .outputList(outputList)
                .caseResults(caseResults)
                .status(finalStatus)
                .executionTime(maxTime)
                .memoryUsed(maxMemory)
                .errorMessage(finalErrorMessage)
                .compileMessage(StrUtil.emptyToNull(compileMessage))
                .build();
    }

    private long mbToBytes(int memoryLimitMb) {
        return (long) memoryLimitMb * 1024 * 1024;
    }

    private DockerLanguageConfig getLanguageConfig(String language) {
        String normalizedLanguage = language.trim().toLowerCase(Locale.ROOT);
        switch (normalizedLanguage) {
            case "java":
                return new DockerLanguageConfig(
                        "Main.java",
                        JAVA_DOCKER_IMAGE,
                        new String[]{"javac", "-encoding", "UTF-8", "Main.java"},
                        memoryLimitMb -> new String[]{"java", "-Xmx" + Math.max(32, memoryLimitMb / 2) + "m",
                                "-Dfile.encoding=UTF-8", "-cp", CONTAINER_APP_DIR, "Main"},
                        true,
                        false,
                        1000L,
                        128
                );
            case "python":
            case "python3":
                return new DockerLanguageConfig(
                        "main.py",
                        "docker.1panel.live/library/python:3.9-alpine",
                        null,
                        memoryLimitMb -> new String[]{"python", "main.py"},
                        false,
                        true,
                        1000L,
                        64
                );
            case "c":
                return new DockerLanguageConfig(
                        "main.c",
                        "docker.1panel.live/library/gcc:12.2.0",
                        new String[]{"gcc", "main.c", "-O2", "-o", "main"},
                        memoryLimitMb -> new String[]{"./main"},
                        true,
                        true,
                        200L,
                        64
                );
            case "cpp":
            case "c++":
                return new DockerLanguageConfig(
                        "main.cpp",
                        "docker.1panel.live/library/gcc:12.2.0",
                        new String[]{"g++", "main.cpp", "-O2", "-std=c++17", "-o", "main"},
                        memoryLimitMb -> new String[]{"./main"},
                        true,
                        true,
                        200L,
                        64
                );
            default:
                throw new IllegalArgumentException("暂不支持的语言: " + language);
        }
    }

    private static class DockerLanguageConfig {

        private final String sourceFileName;
        private final String image;
        private final String[] compileCommand;
        private final Function<Integer, String[]> runCommandFactory;
        private final boolean needCompile;
        private final boolean allowPull;
        private final long startupGraceMillis;
        private final int minMemoryLimitMb;

        private DockerLanguageConfig(String sourceFileName, String image, String[] compileCommand,
                                     Function<Integer, String[]> runCommandFactory, boolean needCompile,
                                     boolean allowPull, long startupGraceMillis, int minMemoryLimitMb) {
            this.sourceFileName = sourceFileName;
            this.image = image;
            this.compileCommand = compileCommand;
            this.runCommandFactory = runCommandFactory;
            this.needCompile = needCompile;
            this.allowPull = allowPull;
            this.startupGraceMillis = startupGraceMillis;
            this.minMemoryLimitMb = minMemoryLimitMb;
        }

        public String getSourceFileName() {
            return sourceFileName;
        }

        public String getImage() {
            return image;
        }

        public String[] getCompileCommand() {
            return compileCommand;
        }

        public boolean needCompile() {
            return needCompile;
        }

        public boolean allowPull() {
            return allowPull;
        }

        public String[] buildRunCommand(int memoryLimitMb) {
            return runCommandFactory.apply(memoryLimitMb);
        }

        public int resolveMemoryLimitMb(Integer requestedMemoryLimitMb) {
            int base = requestedMemoryLimitMb != null && requestedMemoryLimitMb > 0
                    ? requestedMemoryLimitMb : DEFAULT_MEMORY_LIMIT_MB;
            return Math.max(base, minMemoryLimitMb);
        }

        public long resolveTimeoutMillis(Integer requestedTimeLimitMillis) {
            long base = requestedTimeLimitMillis != null && requestedTimeLimitMillis > 0
                    ? requestedTimeLimitMillis : DEFAULT_TIME_LIMIT_MILLIS;
            return base + startupGraceMillis;
        }
    }
}
