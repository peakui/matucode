package com.peakui.matucodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.peakui.matucodesandbox.model.ExecuteCodeRequest;
import com.peakui.matucodesandbox.model.ExecuteCodeResponse;
import com.peakui.matucodesandbox.model.ExecuteMessage;
import com.peakui.matucodesandbox.model.JudgeCaseResult;
import com.peakui.matucodesandbox.model.JudgeStatusEnum;
import com.peakui.matucodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long DEFAULT_TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        File userCodeFile = null;
        try {
            List<String> inputList = executeCodeRequest.getInputList();
            if (inputList == null) {
                inputList = Collections.emptyList();
            }
            String code = executeCodeRequest.getCode();

            userCodeFile = saveCodeToFile(code);
            ExecuteMessage compileResult = compileFile(userCodeFile);
            if (compileResult.getExitValue() != null && compileResult.getExitValue() != 0) {
                return buildCompileErrorResponse(compileResult);
            }

            List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
            return getOutputResponse(executeMessageList, compileResult.getMessage());
        } catch (Exception e) {
            log.error("execute code error", e);
            return getSystemErrorResponse(e);
        } finally {
            if (userCodeFile != null) {
                boolean deleted = deleteFile(userCodeFile);
                if (!deleted) {
                    log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
                }
            }
        }
    }

    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            return ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
        } catch (Exception e) {
            throw new RuntimeException("编译错误", e);
        }
    }

    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                Thread watchdog = new Thread(() -> {
                    try {
                        Thread.sleep(DEFAULT_TIME_OUT);
                        if (runProcess.isAlive()) {
                            runProcess.destroy();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                watchdog.setDaemon(true);
                watchdog.start();

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList, String compileMessage) {
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

    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            return FileUtil.del(userCodeParentPath);
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
}
