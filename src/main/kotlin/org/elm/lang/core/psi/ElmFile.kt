package org.elm.lang.core.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.stubs.ElmFileStub
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.toPsiFile
import org.elm.workspace.ElmProject
import org.elm.workspace.elmWorkspace

private val IS_IN_TESTS_DIRECTORY_KEY: Key<CachedValue<Boolean>> = Key.create("IS_IN_TESTS_DIRECTORY_KEY")

class ElmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElmLanguage), ClientLocation {

    override fun getFileType() =
            ElmFileType

    override fun toString() =
            "Elm File"

    override fun getStub(): ElmFileStub? =
            super.getStub() as ElmFileStub?

    fun isCore(): Boolean = elmProject?.isCore() ?: false

    override fun setName(name: String): PsiElement {
        val nameWithExtension = if (name.endsWith(".elm")) name else "$name.elm"
        return super.setName(nameWithExtension)
    }

    override val intellijProject: Project
        get() = project

    override val elmProject: ElmProject?
        // Must use [originalFile] because during code completion the direct [virtualFile] is an
        // in-memory copy of the real file. See:
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000108704/comments/115000123710
        get() = project.elmWorkspace.findProjectForFile(originalFile.virtualFile)

    override val isInTestsDirectory: Boolean
        // This is called often during reference resolve, and the system-independent path comparison
        // is slow enough that the result is worth caching
        get() = CachedValuesManager.getCachedValue(this, IS_IN_TESTS_DIRECTORY_KEY) {
            elmProject?.let { elmProj ->
                val res = originalFile.virtualFile?.pathAsPath?.startsWith(elmProj.testsDirPath)
                CachedValueProvider.Result.create(res, globalModificationTracker)
            }
        }

    fun getModuleDecl(): ElmModuleDeclaration? {
        if (stub != null) return stubDirectChildrenOfType<ElmModuleDeclaration>().firstOrNull()
        // No need to generate the list of all children if we aren't a stub
        return directChildren.filterIsInstance<ElmModuleDeclaration>().firstOrNull()
    }

    fun getImportClauses(): List<ElmImportClause> {
        if (stub != null) return stubDirectChildrenOfType()

        // If we aren't a stub, we can optimize this search to take advantage of the fact that
        // imports can't occur after declarations, so we don't need to look through the whole tree.
        return directChildren.withoutWsOrComments.withoutErrors
                .takeWhile { it is ElmModuleDeclaration || it is ElmImportClause || it.elementType == ElmTypes.AS }
                .filterIsInstance<ElmImportClause>()
                .toList()
    }

    companion object {
        fun fromVirtualFile(file: VirtualFile, project: Project): ElmFile? =
                file.toPsiFile(project) as? ElmFile
    }
}

val VirtualFile.isElmFile
    get() = fileType == ElmFileType
