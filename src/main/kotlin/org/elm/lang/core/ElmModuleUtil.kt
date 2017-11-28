package org.elm.lang.core

import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.elm.lang.core.psi.ElmFile


fun modulePathToFile(moduleName: String, project: Project): ElmFile? {
    val (relativeModulePath, filename) = toFileSystemParts(moduleName)

    if (relativeModulePath == "Native") // TODO [kl] is this the right place to handle JS files?
        return null

    // TODO [kl] re-visit this choice for `GlobalSearchScope`
    val scope = GlobalSearchScope.projectScope(project)

    val pathSuffix = if (relativeModulePath.isNotEmpty())
                        "/src/" + relativeModulePath + "/" + filename
                     else
                        "/src/" + filename

    val file = FilenameIndex.getFilesByName(project, filename, scope)
            .find { it.virtualFile.path.endsWith(pathSuffix)}
            ?: return null

//    println("finding module with pathSuffix=$pathSuffix, got=$file")

    if (file !is ElmFile)
        error("must be an Elm file")

    return file
}

private fun toFileSystemParts(moduleName: String): Pair<String, String> {
    val parts = moduleName.split(".")
    val relativeModulePath = parts.dropLast(1).joinToString("/")
    val filename = parts.last() + ".elm"
    return Pair(relativeModulePath, filename)
}
