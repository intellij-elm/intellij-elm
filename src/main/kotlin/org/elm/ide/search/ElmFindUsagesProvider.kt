package org.elm.ide.search

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.elm.lang.core.lexer.ElmIncrementalLexer
import org.elm.lang.core.psi.ELM_COMMENTS
import org.elm.lang.core.psi.ELM_IDENTIFIERS
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.REGULAR_STRING_PART
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.tokenSetOf


class ElmFindUsagesProvider : FindUsagesProvider {


    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                ElmIncrementalLexer(),
                ELM_IDENTIFIERS,
                ELM_COMMENTS,
                tokenSetOf(REGULAR_STRING_PART),
                TokenSet.EMPTY,
                tokenSetOf(OPERATOR_IDENTIFIER))
    }


    override fun canFindUsagesFor(psiElement: PsiElement) =
            psiElement is ElmNamedElement


    override fun getType(element: PsiElement): String {
        // TODO [kl] handle more cases
        return when (element) {
            is ElmModuleDeclaration -> "Module"
            is ElmAsClause -> "Aliased Module Import"
            is ElmFunctionDeclarationLeft -> "Value/Function Declaration"
            is ElmInfixDeclaration -> "Infix Operator Declaration"
            is ElmTypeAliasDeclaration -> "Type Alias"
            is ElmTypeDeclaration -> "Union Type"
            is ElmUnionVariant -> "Union Variant"
            is ElmLowerPattern -> "Value Binding"
            is ElmPortAnnotation -> "Port Annotation"
            is ElmFieldType -> "Record Field"
            else -> "unknown type for $element"
        }
    }


    // TODO [kl] this doesn't appear to do anything?
    override fun getDescriptiveName(element: PsiElement) =
            when (element) {
                is ElmNamedElement -> element.name ?: "unknown"
                else -> "unknown descriptive name for $element"
            }


    override fun getNodeText(element: PsiElement, useFullName: Boolean) =
            when (element) {
                is ElmNamedElement -> element.name ?: "unknown"
                else -> "unknown node text for $element"
            }


    override fun getHelpId(psiElement: PsiElement) =
            null
}
