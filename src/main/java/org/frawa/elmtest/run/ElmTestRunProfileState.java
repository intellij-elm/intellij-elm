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
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.events.*;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.util.Key;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor;
import org.frawa.elmtest.core.ElmTestJsonProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ElmTestRunProfileState extends CommandLineState {

    private final ElmTestRunConfiguration configuration;

    protected ElmTestRunProfileState(ExecutionEnvironment environment, ElmTestRunConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        String elmFolder = getElmFolder();
        String elmTestBinary = getElmTestBinary(elmFolder);
        String elmBinary = getElmBinary(elmTestBinary);

        GeneralCommandLine commandLine = elmTestBinary != null
                ? new GeneralCommandLine(elmTestBinary, "--report=json")
                : new GeneralCommandLine("/bin/sh", "-i", "-c", "elm-test --report=json");

        commandLine
                .withWorkDirectory(elmFolder)
                .withRedirectErrorStream(true)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        if (elmTestBinary != null && elmBinary != null) {
            commandLine.withParameters("--compiler", elmBinary);
        }

        return new ColoredProcessHandler(commandLine);
    }

    @SystemIndependent
    private String getElmFolder() {
        return configuration.options.elmFolder == null || configuration.options.elmFolder.isEmpty()
                ? this.getEnvironment().getProject().getBasePath()
                : configuration.options.elmFolder;
    }

    private String getElmTestBinary(String elmFolder) {
        if (configuration.options.elmTestBinary == null || configuration.options.elmTestBinary.isEmpty()) {
            return getLocalElmTestBinary(elmFolder);
        }
        return configuration.options.elmTestBinary;
    }

    private String getLocalElmTestBinary(String elmFolder) {
        if (elmFolder != null) {
            Path elmFolderPath = Paths.get(elmFolder);
            Path elmTestPath = elmFolderPath.resolve("node_modules/.bin/elm-test");
            if (elmTestPath.toFile().exists()) {
                return elmTestPath.toString();
            }
        }
        return null;
    }

    private String getElmBinary(String elmTestBinary) {
        if (elmTestBinary != null) {
            Path elmTestPath = Paths.get(elmTestBinary);
            Path elmPath = elmTestPath.resolveSibling("elm");
            if (elmPath.toFile().exists()) {
                return elmPath.toString();
            }
        }
        return null;
    }

    @Nullable
    @Override
    protected ConsoleView createConsole(@NotNull Executor executor) {
        RunConfiguration runConfiguration = (RunConfiguration) this.getEnvironment().getRunProfile();
        TestConsoleProperties properties = new ConsoleProperties(runConfiguration, executor);
        SMTRunnerConsoleView consoleView = new SMTRunnerConsoleView(properties);
        SMTestRunnerConnectionUtil.initConsoleView(consoleView, ((SMTRunnerConsoleProperties) properties).getTestFrameworkName());
        return consoleView;
    }

    private static class ConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {

        public ConsoleProperties(@NotNull RunConfiguration config, @NotNull Executor executor) {
            super(config, "elm-test", executor);

            setIfUndefined(TestConsoleProperties.TRACK_RUNNING_TEST, true);
            setIfUndefined(TestConsoleProperties.OPEN_FAILURE_LINE, true);
            setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false);
            setIfUndefined(TestConsoleProperties.SHOW_STATISTICS, true);
            setIfUndefined(TestConsoleProperties.SELECT_FIRST_DEFECT, true);
            setIfUndefined(TestConsoleProperties.SCROLL_TO_SOURCE, true);
//            INCLUDE_NON_STARTED_IN_RERUN_FAILED
//            setIdBasedTestTree(true);
//            setPrintTestingStartedTime(false);
        }


        @Override
        public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties consoleProperties) {
            return new OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {
                ElmTestJsonProcessor processor = new ElmTestJsonProcessor();

                @Override
                public synchronized void finishTesting() {
                    super.finishTesting();
                }

                @Override
                protected boolean processServiceMessages(String text, Key outputType, ServiceMessageVisitor visitor) {
                    List<TreeNodeEvent> events = processor.accept(text);
                    if (events == null) {
                        return false;
                    }
                    events.stream().forEach(this::processEvent);
                    return true;
                }

                private void processEvent(TreeNodeEvent event) {
                    if (event instanceof TestStartedEvent) {
                        this.getProcessor().onTestStarted((TestStartedEvent) event);
                    } else if (event instanceof TestFinishedEvent) {
                        this.getProcessor().onTestFinished((TestFinishedEvent) event);
                    } else if (event instanceof TestFailedEvent) {
                        this.getProcessor().onTestFailure((TestFailedEvent) event);
                    } else if (event instanceof TestIgnoredEvent) {
                        this.getProcessor().onTestIgnored((TestIgnoredEvent) event);
                    } else if (event instanceof TestSuiteStartedEvent) {
                        this.getProcessor().onSuiteStarted((TestSuiteStartedEvent) event);
                    } else if (event instanceof TestSuiteFinishedEvent) {
                        this.getProcessor().onSuiteFinished((TestSuiteFinishedEvent) event);
                    }
                }
            };
        }

        @Nullable
        @Override
        public SMTestLocator getTestLocator() {
            return ElmTestLocator.INSTANCE;
        }
    }
}
