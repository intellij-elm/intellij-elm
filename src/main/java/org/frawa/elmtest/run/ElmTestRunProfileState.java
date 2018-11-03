package org.frawa.elmtest.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElmTestRunProfileState extends CommandLineState {

    protected ElmTestRunProfileState(ExecutionEnvironment environment) {
        super(environment);
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
//        GeneralCommandLine commandLine = new GeneralCommandLine("/usr/local/bin/elm", "test")
//        GeneralCommandLine commandLine = new GeneralCommandLine("/bin/sh", "-i", "-c", "elm test")
        GeneralCommandLine commandLine = new GeneralCommandLine("/usr/bin/script", "-q", "/dev/null", "elm", "test")
                .withWorkDirectory(this.getEnvironment().getProject().getBasePath())
                .withRedirectErrorStream(true)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withEnvironment("TERM", "xterm-256color")
                .withEnvironment("COLORTERM", "truecolor");
        return new ColoredProcessHandler(commandLine);
    }
}
