package org.elm.ide.test.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentDescriptorReusePolicy
import org.jdom.Element

class ElmTestProgramRunner : GenericProgramRunner<Settings>() {

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val result = state.execute(environment.executor, this) ?: return null
        return RunContentBuilder(result, environment)
                .showRunContent(environment.contentToReuse)
                .apply {
                    reusePolicy = object : RunContentDescriptorReusePolicy() {
                        override fun canBeReusedBy(newDescriptor: RunContentDescriptor) = true
                    }
                }
    }

    override fun getRunnerId() = "ELM_TEST_PROGRAM_RUNNER"

    override fun canRun(executorId: String, profile: RunProfile) =
            DefaultRunExecutor.EXECUTOR_ID == executorId && profile is ElmTestRunConfiguration
}

class Settings : RunnerSettings {
    override fun writeExternal(element: Element?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readExternal(element: Element?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
