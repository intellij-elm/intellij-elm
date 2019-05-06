package org.elm.ide.inspections.fixes

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.elm.ide.code.format.ElmImportOptimizer
import org.elm.lang.core.psi.ElmFile

class OptimizeImportsFix : LocalQuickFix, IntentionAction, HighPriorityAction {
    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = file is ElmFile

    override fun getText() = name

    override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        optimizeImports(project, file)
    }

    override fun getName() = "Optimize imports"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement?.containingFile as? ElmFile ?: return

        optimizeImports(project, file)
    }

    private fun optimizeImports(project: Project, file: PsiFile) {
        val optimizer = ElmImportOptimizer()

        val runnable = optimizer.processFile(file)

        WriteCommandAction
                .writeCommandAction(project, file)
                .withName(familyName)
                .run<Exception> { runnable.run()  }
    }
}