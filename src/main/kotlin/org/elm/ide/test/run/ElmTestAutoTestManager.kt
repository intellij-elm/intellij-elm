package org.elm.ide.test.run

import com.intellij.execution.testframework.autotest.AbstractAutoTestManager
import com.intellij.execution.testframework.autotest.AutoTestWatcher
import com.intellij.execution.testframework.autotest.DelayedDocumentWatcher
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.elm.lang.core.ElmFileType

@State(name = "ElmTestAutoTestManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ElmTestAutoTestManager internal constructor(project: Project) : AbstractAutoTestManager(project) {

    override fun createWatcher(project: Project): AutoTestWatcher {
        return DelayedDocumentWatcher(project,
                myDelayMillis,
                { this.restartAllAutoTests(it) },
                { file -> FileEditorManager.getInstance(project).isFileOpen(file) && ElmFileType == file.fileType }
        )
    }

    companion object {

        internal fun getInstance(project: Project): ElmTestAutoTestManager {
            return ServiceManager.getService(project, ElmTestAutoTestManager::class.java)
        }
    }

}
