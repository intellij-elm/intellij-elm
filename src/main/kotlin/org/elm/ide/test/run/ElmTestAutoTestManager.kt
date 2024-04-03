package org.elm.ide.test.run

import com.intellij.execution.testframework.autotest.AbstractAutoTestManager
import com.intellij.execution.testframework.autotest.DelayedDocumentWatcher
import com.intellij.openapi.components.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.elm.lang.core.ElmFileType
import java.util.function.IntConsumer

@State(
    name = "ElmTestAutoTestManager",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class ElmTestAutoTestManager internal constructor(
    project: Project
) : AbstractAutoTestManager(project) {

    override fun createWatcher(project: Project) =
        DelayedDocumentWatcher(project,
            myDelayMillis,
            IntConsumer { value -> restartAllAutoTests(value) }
        ) { it.fileType == ElmFileType && FileEditorManager.getInstance(project).isFileOpen(it) }
}

val Project.elmAutoTestManager
    get() = service<ElmTestAutoTestManager>()
