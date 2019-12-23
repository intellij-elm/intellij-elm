package org.elm.ide.test.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.autotest.AbstractAutoTestManager
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.events.*
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor
import org.elm.ide.test.core.ElmProjectTestsHelper
import org.elm.ide.test.core.ElmTestJsonProcessor
import org.elm.workspace.ElmWorkspaceService
import java.nio.file.Files

class ElmTestRunProfileState internal constructor(environment: ExecutionEnvironment, private val configuration: ElmTestRunConfiguration) : CommandLineState(environment) {

    private val elmFolder: String?
        get() = if (configuration.options.elmFolder == null || configuration.options.elmFolder!!.isEmpty())
            this.environment.project.basePath
        else
            configuration.options.elmFolder

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        FileDocumentManager.getInstance().saveAllDocuments()
        val project = environment.project
        val workspaceService = ServiceManager.getService(project, ElmWorkspaceService::class.java)

        val toolchain = workspaceService.settings.toolchain
        val elmTestCLI = toolchain.elmTestCLI
        val elmCompilerBinary = toolchain.elmCompilerPath

        if (elmTestCLI == null) {
            return handleBadConfiguration(workspaceService, "Could not find elm-test")
        }
        if (elmCompilerBinary == null) {
            return handleBadConfiguration(workspaceService, "Could not find the Elm compiler")
        }

        val elmFolder = elmFolder

        val adjusted = ElmProjectTestsHelper(project)
                .adjustElmCompilerProjectDirPath(elmFolder!!, elmCompilerBinary)

        return if (!Files.exists(adjusted)) {
            handleBadConfiguration(workspaceService, "Could not find the Elm compiler (elm-make)")
        } else elmTestCLI.runTestsProcessHandler(adjusted, elmFolder)

    }

    @Throws(ExecutionException::class)
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val result = super.execute(executor, runner)
        if (result is DefaultExecutionResult) {
            result.setRestartActions(object : ToggleAutoTestAction() {
                override fun getAutoTestManager(project: Project): AbstractAutoTestManager {
                    return ElmTestAutoTestManager.getInstance(project)
                }
            })
        }
        return result
    }

    @Throws(ExecutionException::class)
    private fun handleBadConfiguration(workspaceService: ElmWorkspaceService, errorMessage: String): ProcessHandler {
        // TODO when this code gets ported to Kotlin, use org.elm.ide.notifications.UtilsKt.showBalloon
        val group = NotificationGroup.balloonGroup("Elm Plugin")
        val notification = group.createNotification(errorMessage, NotificationType.ERROR)
        notification.addAction(NotificationAction.createSimple("Fix"
        ) {
            notification.hideBalloon()
            workspaceService.showConfigureToolchainUI()
        })
        Notifications.Bus.notify(notification, environment.project)
        throw ExecutionException(errorMessage)
    }

    override fun createConsole(executor: Executor): ConsoleView? {
        val runConfiguration = this.environment.runProfile as RunConfiguration
        val properties = ConsoleProperties(runConfiguration, executor)
        val consoleView = SMTRunnerConsoleView(properties)
        SMTestRunnerConnectionUtil.initConsoleView(consoleView, properties.testFrameworkName)
        return consoleView
    }

    private class ConsoleProperties internal constructor(config: RunConfiguration, executor: Executor) : SMTRunnerConsoleProperties(config, "elm-test", executor), SMCustomMessagesParsing {

        init {

            setIfUndefined(TestConsoleProperties.TRACK_RUNNING_TEST, true)
            setIfUndefined(TestConsoleProperties.OPEN_FAILURE_LINE, true)
            setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false)
            setIfUndefined(TestConsoleProperties.SHOW_STATISTICS, true)
            setIfUndefined(TestConsoleProperties.SELECT_FIRST_DEFECT, true)
            setIfUndefined(TestConsoleProperties.SCROLL_TO_SOURCE, true)
            //            INCLUDE_NON_STARTED_IN_RERUN_FAILED
            //            setIdBasedTestTree(true);
            //            setPrintTestingStartedTime(false);
        }


        override fun createTestEventsConverter(testFrameworkName: String, consoleProperties: TestConsoleProperties): OutputToGeneralTestEventsConverter {
            return object : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {
                var processor = ElmTestJsonProcessor()

                @Synchronized
                override fun finishTesting() {
                    super.finishTesting()
                }

                override fun processServiceMessages(text: String, outputType: Key<*>?, visitor: ServiceMessageVisitor): Boolean {
                    val events = processor.accept(text) ?: return false
                    events.forEach { processEvent(it) }
                    return true
                }

                private fun processEvent(event: TreeNodeEvent) {
                    if (event is TestStartedEvent) {
                        this.getProcessor().onTestStarted(event)
                    } else if (event is TestFinishedEvent) {
                        this.getProcessor().onTestFinished(event)
                    } else if (event is TestFailedEvent) {
                        this.getProcessor().onTestFailure(event)
                    } else if (event is TestIgnoredEvent) {
                        this.getProcessor().onTestIgnored(event)
                    } else if (event is TestSuiteStartedEvent) {
                        this.getProcessor().onSuiteStarted(event)
                    } else if (event is TestSuiteFinishedEvent) {
                        this.getProcessor().onSuiteFinished(event)
                    }
                }
            }
        }

        override fun getTestLocator(): SMTestLocator? {
            return ElmTestLocator.INSTANCE
        }
    }
}
