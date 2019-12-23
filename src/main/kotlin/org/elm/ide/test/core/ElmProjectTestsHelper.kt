package org.elm.ide.test.core

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.elm.workspace.ElmProject
import org.elm.workspace.ElmWorkspaceService
import java.nio.file.Files
import java.nio.file.Path

class ElmProjectTestsHelper(project: Project) {

    private val workspaceService: ElmWorkspaceService

    private val testableProjects: Sequence<ElmProject>
        get() = workspaceService.allProjects.asSequence()
                .filter { Files.exists(it.projectDirPath.resolve("tests")) }

    init {
        workspaceService = ServiceManager.getService(project, ElmWorkspaceService::class.java)
    }

    fun allNames(): Sequence<String> {
        return testableProjects
                .map { it.presentableName }
    }

    fun nameByProjectDirPath(path: String): String? {
        return testableProjects
                .filter { it.projectDirPath.toString() == path }
                .map { it.presentableName }
                .firstOrNull()
    }

    fun projectDirPathByName(name: String): String? {
        return testableProjects
                .filter { it.presentableName == name }
                .map { it.projectDirPath }
                .map { it.toString() }
                .firstOrNull()
    }

    fun elmProjectByProjectDirPath(path: String): ElmProject? {
        return testableProjects
                .filter { it.projectDirPath.toString() == path }
                .firstOrNull()
    }

    fun adjustElmCompilerProjectDirPath(elmFolder: String, compilerPath: Path): Path {
        // TODO [drop 0.18] this function can be removed
        return if (elmProjectByProjectDirPath(elmFolder)?.isElm18 == true) {
            compilerPath.resolveSibling("elm-make")
        } else {
            compilerPath
        }
    }

    companion object {

        fun elmFolderForTesting(elmProject: ElmProject): Path {
            // TODO [drop 0.18] this function can be removed
            return if (elmProject.isElm18 && elmProject.presentableName == "tests")
                elmProject.projectDirPath.parent
            else
                elmProject.projectDirPath
        }
    }

}
