package org.elm.lang.core.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.stubs.ElmFileStub
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.toPsiFile
import org.elm.workspace.ElmPackageProject
import org.elm.workspace.ElmProject
import org.elm.workspace.elmWorkspace


class ElmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElmLanguage), ClientLocation {

    override fun getFileType() =
            ElmFileType

    override fun toString() =
            "Elm File"

    override fun getIcon(flags: Int) =
            super.getIcon(flags)

    override fun getStub() =
            super.getStub() as ElmFileStub?

    fun isCore(): Boolean {
        val pkgName = (elmProject as? ElmPackageProject)?.name
                ?: return false
        return (pkgName == "elm/core" || pkgName == "elm-lang/core") // TODO [drop 0.18] remove "elm-core/lang" clause
    }

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
        get() {
            val elmProj = elmProject ?: return false
            val vfile = originalFile.virtualFile ?: return false
            return vfile.pathAsPath.startsWith(elmProj.testsDirPath)
        }

    fun getModuleDecl() =
            stubDirectChildrenOfType<ElmModuleDeclaration>().firstOrNull()

    fun getImportClauses() =
            stubDirectChildrenOfType<ElmImportClause>()

    fun getValueDeclarations() =
            stubDirectChildrenOfType<ElmValueDeclaration>()

    fun getTypeDeclarations() =
            stubDirectChildrenOfType<ElmTypeDeclaration>()

    fun getTypeAliasDeclarations() =
            stubDirectChildrenOfType<ElmTypeAliasDeclaration>()

    fun getTypeAnnotations() =
            stubDirectChildrenOfType<ElmTypeAnnotation>()

    fun getPortAnnotations() =
            stubDirectChildrenOfType<ElmPortAnnotation>()

    fun getInfixDeclarations() =
            stubDirectChildrenOfType<ElmInfixDeclaration>()


    companion object {
        fun fromVirtualFile(file: VirtualFile, project: Project): ElmFile? =
                file.toPsiFile(project) as? ElmFile
    }
}

val VirtualFile.isElmFile
    get() = fileType == ElmFileType
