package org.frawa.elmtest.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.RunConfiguration;
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
import com.intellij.notification.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Key;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor;
import org.elm.workspace.ElmToolchain;
import org.elm.workspace.ElmWorkspaceService;
import org.elm.workspace.commandLineTools.ElmTestCLI;
import org.frawa.elmtest.core.ElmTestJsonProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import java.nio.file.Path;
import java.util.List;

public class ElmTestRunProfileState extends CommandLineState {

    private final ElmTestRunConfiguration configuration;

    ElmTestRunProfileState(ExecutionEnvironment environment, ElmTestRunConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();
        ElmWorkspaceService workspaceService = ServiceManager.getService(getEnvironment().getProject(), ElmWorkspaceService.class);

        ElmToolchain toolchain = workspaceService.getSettings().getToolchain();
        ElmTestCLI elmTestCLI = toolchain.getElmTestCLI();
        Path elmCompilerBinary = toolchain.getElmCompilerPath();
        if (elmTestCLI == null) {
            return handleBadConfiguration(workspaceService, "Could not find elm-test");
        }
        if (elmCompilerBinary == null) {
            return handleBadConfiguration(workspaceService, "Could not find the Elm compiler");
        }
        return elmTestCLI.runTestsProcessHandler(elmCompilerBinary, getElmFolder());
    }

    private ProcessHandler handleBadConfiguration(ElmWorkspaceService workspaceService, String errorMessage) throws ExecutionException {
        // TODO when this code gets ported to Kotlin, use org.elm.ide.notifications.UtilsKt.showBalloon
        NotificationGroup group = NotificationGroup.balloonGroup("Elm Plugin");
        Notification notification = group.createNotification(errorMessage, NotificationType.ERROR);
        notification.addAction(NotificationAction.createSimple("Fix",
                () -> {
                    notification.hideBalloon();
                    workspaceService.showConfigureToolchainUI();
                }));
        Notifications.Bus.notify(notification, getEnvironment().getProject());
        throw new ExecutionException(errorMessage);
    }

    @SystemIndependent
    private String getElmFolder() {
        return configuration.options.elmFolder == null || configuration.options.elmFolder.isEmpty()
                ? this.getEnvironment().getProject().getBasePath()
                : configuration.options.elmFolder;
    }

    @Nullable
    @Override
    protected ConsoleView createConsole(@NotNull Executor executor) {
        RunConfiguration runConfiguration = (RunConfiguration) this.getEnvironment().getRunProfile();
        SMTRunnerConsoleProperties properties = new ConsoleProperties(runConfiguration, executor);
        SMTRunnerConsoleView consoleView = new SMTRunnerConsoleView(properties);
        SMTestRunnerConnectionUtil.initConsoleView(consoleView, properties.getTestFrameworkName());
        return consoleView;
    }

    private static class ConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {

        ConsoleProperties(@NotNull RunConfiguration config, @NotNull Executor executor) {
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
