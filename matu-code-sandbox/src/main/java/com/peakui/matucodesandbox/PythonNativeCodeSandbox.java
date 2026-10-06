package com.peakui.matucodesandbox;

import com.peakui.matucodesandbox.model.ExecuteMessage;
import com.peakui.matucodesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class PythonNativeCodeSandbox extends NativeCodeSandboxTemplate {

    @Override
    protected String getSourceFileName() {
        return "main.py";
    }

    @Override
    protected ExecuteMessage compileFile(File userCodeFile) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        executeMessage.setExitValue(0);
        executeMessage.setTime(0L);
        executeMessage.setTimeout(false);
        return executeMessage;
    }

    @Override
    protected List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("python \"%s\" %s", userCodeParentPath + File.separator + getSourceFileName(), inputArgs == null ? "" : inputArgs);
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
}
