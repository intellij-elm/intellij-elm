package org.frawa.elmtest.run.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

public class ElmTestRunProfileState extends CommandLineState {

    protected ElmTestRunProfileState(ExecutionEnvironment environment) {
        super(environment);
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        GeneralCommandLine commandLine = new GeneralCommandLine("/bin/sh", "-c", "ls");
        commandLine.setWorkDirectory(this.getEnvironment().getProject().getBasePath());
        commandLine.withRedirectErrorStream(true);
        return new ColoredProcessHandler(commandLine);
    }
}
