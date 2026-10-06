package com.peakui.matucodesandbox;

import com.peakui.matucodesandbox.model.ExecuteMessage;
import com.peakui.matucodesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CNativeCodeSandbox extends NativeCodeSandboxTemplate {

    @Override
    protected String getSourceFileName() {
        return "main.c";
    }

    @Override
    protected ExecuteMessage compileFile(File userCodeFile) {
        String exePath = getExecutablePath(userCodeFile);
        String compileCmd = String.format("gcc \"%s\" -o \"%s\"", userCodeFile.getAbsolutePath(), exePath);
        return compileByCommand(compileCmd);
    }

    @Override
    protected List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String exePath = getExecutablePath(userCodeFile);
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("\"%s\" %s", exePath, inputArgs == null ? "" : inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    private String getExecutablePath(File userCodeFile) {
        return userCodeFile.getParentFile().getAbsolutePath() + File.separator + "main.exe";
    }
}
