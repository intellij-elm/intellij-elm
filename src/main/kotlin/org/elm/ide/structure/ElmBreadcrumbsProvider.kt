package org.elm.ide.structure

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.*


class ElmBreadcrumbsProvider : BreadcrumbsProvider {
    override fun getLanguages(): Array<Language> = LANGUAGES

    override fun acceptElement(element: PsiElement): Boolean {
        return element is ElmPsiElement && breadcrumbName(element) != null
    }

    override fun getElementInfo(element: PsiElement): String {
        return breadcrumbName(element as ElmPsiElement)!!
    }

    companion object {
        private val LANGUAGES: Array<Language> = arrayOf(ElmLanguage)

        fun breadcrumbName(e: ElmPsiElement): String? {
            return when (e) {
                is ElmLetInExpr -> "let … in"
                is ElmIfElseExpr -> "if ${e.expressionList.firstOrNull()?.text.truncate()} then"
                is ElmTypeAliasDeclaration -> e.name
                is ElmTypeDeclaration -> e.name
                is ElmTypeAnnotation -> "${e.referenceName} :"
                is ElmValueDeclaration -> when (val assignee = e.assignee) {
                    is ElmFunctionDeclarationLeft -> assignee.name
                    else -> assignee?.text?.truncate()
                }
                is ElmAnonymousFunctionExpr -> e.patternList.joinToString(" ", prefix = "\\") { it.text }.truncate() + " ->"
                is ElmFieldType -> e.name
                is ElmUnionVariant -> e.name
                is ElmCaseOfExpr -> "case ${e.expression?.text.truncate()} of"
                is ElmCaseOfBranch -> "${e.pattern.text.truncate()} ->"
                is ElmRecordExpr -> e.baseRecordIdentifier?.let { "{${it.text} | …}" }
                else -> null
            }
        }

        private fun String?.truncate(len: Int = 20) = when {
            this == null -> "…"
            length > len -> take(len) + "…"
            else -> this
        }
    }
}

