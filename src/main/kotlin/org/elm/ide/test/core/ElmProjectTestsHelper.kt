package org.elm.ide.test.core

import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import org.elm.workspace.ElmProject
import org.elm.workspace.elmWorkspace

class ElmProjectTestsHelper(project: Project) {

    private val elmWorkspace = project.elmWorkspace

    private val testableProjects: List<ElmProject>
        get() = elmWorkspace.allProjects
                .filter { it.testsDirPath.exists() }

    fun allNames() = testableProjects.map { it.presentableName }

    fun nameByProjectDirPath(path: String): String? {
        return testableProjects
                .filter { it.projectDirPath.toString() == path }
                .map { it.presentableName }
                .firstOrNull()
    }

    fun projectDirPathByName(name: String): String? {
        return testableProjects
                .filter { it.presentableName == name }
                .map { it.projectDirPath.toString() }
                .firstOrNull()
    }

    fun elmProjectByProjectDirPath(path: String): ElmProject? {
        return testableProjects
                .firstOrNull { it.projectDirPath.toString() == path }
    }
}
