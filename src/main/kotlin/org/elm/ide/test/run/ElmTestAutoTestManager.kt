package org.elm.ide.test.run

import com.intellij.execution.testframework.autotest.AbstractAutoTestManager
import com.intellij.execution.testframework.autotest.DelayedDocumentWatcher
import com.intellij.openapi.components.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import org.elm.lang.core.ElmFileType

@State(
    name = "ElmTestAutoTestManager",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class ElmTestAutoTestManager internal constructor(
    project: Project
) : AbstractAutoTestManager(project) {

    override fun createWatcher(project: Project) =
        // This is the only constructor that is available both in Platform versions 2022.2.4 and master
        DelayedDocumentWatcher(project,
            myDelayMillis,
            Consumer { value: Int -> restartAllAutoTests(value) },
            Condition { it: VirtualFile -> it.fileType == ElmFileType && FileEditorManager.getInstance(project).isFileOpen(it) }
        )
}

val Project.elmAutoTestManager
    get() = service<ElmTestAutoTestManager>()
