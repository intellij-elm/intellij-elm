package org.elm.ide.refactoring

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.usageView.UsageInfo
import org.elm.ide.actions.ElmCreateFileAction
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.openapiext.pathAsPath
import org.elm.workspace.ElmProject
import java.nio.file.Path

class ElmMoveFileHandler : MoveFileHandler() {

    override fun canProcessElement(element: PsiFile?): Boolean {
        return ((element as? ElmFile?) != null)
    }

    override fun prepareMovedFile(
        file: PsiFile?,
        moveDestination: PsiDirectory?,
        oldToNewMap: MutableMap<PsiElement, PsiElement>?
    ) {
    }

    override fun findUsages(
        psiFile: PsiFile?,
        newParent: PsiDirectory?,
        searchInComments: Boolean,
        searchInNonJavaFiles: Boolean
    ): MutableList<UsageInfo> {
        return emptyList<UsageInfo>().toMutableList()
    }

    override fun retargetUsages(usageInfos: MutableList<UsageInfo>?, oldToNewMap: MutableMap<PsiElement, PsiElement>?) {
    }

    override fun updateMovedFile(file: PsiFile?) {
        val elmFile: ElmFile = file as? ElmFile?: return
        val moduleDecl: ElmModuleDeclaration = elmFile.getModuleDecl()?: return
        val project: ElmProject = elmFile.elmProject?: return
        val path: Path = elmFile.parent?.virtualFile?.pathAsPath?: return
        val relativePath =
            project.rootDirContaining(elmFile.virtualFile)?.relativize(path)?.joinToString(".")?: return

        val newModuleDeclaration: ElmModuleDeclaration = ElmPsiFactory(elmFile.project)
            .createElements("module ${relativePath}.${elmFile.name} exposing (..)")
            .first() as ElmModuleDeclaration

        moduleDecl.upperCaseQID.replace(newModuleDeclaration.upperCaseQID)
    }

}
