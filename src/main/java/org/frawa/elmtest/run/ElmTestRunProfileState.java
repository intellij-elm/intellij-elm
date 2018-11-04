package org.frawa.elmtest.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.events.TestFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestStartedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteFinishedEvent;
import com.intellij.execution.testframework.sm.runner.events.TestSuiteStartedEvent;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.util.Key;
import jetbrains.buildServer.messages.serviceMessages.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;

public class ElmTestRunProfileState extends CommandLineState {

    protected ElmTestRunProfileState(ExecutionEnvironment environment) {
        super(environment);
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
//        GeneralCommandLine commandLine = new GeneralCommandLine("/usr/local/bin/elm", "test")
//        GeneralCommandLine commandLine = new GeneralCommandLine("/bin/sh", "-i", "-c", "elm test")
        GeneralCommandLine commandLine = new GeneralCommandLine("/usr/bin/script", "-q", "/dev/null",
                "elm", "test", "--report=junit")
                .withWorkDirectory(this.getEnvironment().getProject().getBasePath())
                .withRedirectErrorStream(true)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withEnvironment("TERM", "xterm-256color")
                .withEnvironment("COLORTERM", "truecolor");
        return new ColoredProcessHandler(commandLine);
    }

    @Nullable
    @Override
    protected ConsoleView createConsole(@NotNull Executor executor) throws ExecutionException {
        RunConfiguration runConfiguration = (RunConfiguration) this.getEnvironment().getRunProfile();
        TestConsoleProperties properties = new ConsoleProperties(runConfiguration, executor);
        SMTRunnerConsoleView consoleView = new SMTRunnerConsoleView(properties);
        SMTestRunnerConnectionUtil.initConsoleView(consoleView, ((SMTRunnerConsoleProperties) properties).getTestFrameworkName());
        return consoleView;
    }

    private static class ConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {

        public ConsoleProperties(@NotNull RunConfiguration config, @NotNull Executor executor) {
            super(config, "elm-test", executor);
        }


        @Override
        public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties consoleProperties) {
            return new OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

                @Override
                public synchronized void finishTesting() {
                    String name = "FW";
                    String test = "a test";

                    this.getProcessor().onSuiteStarted(new TestSuiteStartedEvent(name,null));
                    this.getProcessor().onTestStarted(new TestStartedEvent(test,  null));
                    this.getProcessor().onTestFinished(new TestFinishedEvent(test, 13L));
                    this.getProcessor().onSuiteFinished(new TestSuiteFinishedEvent(name));

                    super.finishTesting();
                }

                @Override
                protected boolean processServiceMessages(String text, Key outputType, ServiceMessageVisitor visitor) throws ParseException {
                    return false;
                }
            };
        }
    }
}
