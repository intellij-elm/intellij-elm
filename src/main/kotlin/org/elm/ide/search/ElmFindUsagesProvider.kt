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
import org.elm.lang.core.psi.ElmTypes.STRING_LITERAL
import org.elm.lang.core.psi.elements.ElmAsClause
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmLowerPattern
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.psi.elements.ElmOperatorDeclarationLeft
import org.elm.lang.core.psi.elements.ElmPatternAs
import org.elm.lang.core.psi.elements.ElmPortAnnotation
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmUnionMember
import org.elm.lang.core.psi.tokenSetOf


class ElmFindUsagesProvider: FindUsagesProvider {


    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                ElmIncrementalLexer(),
                ELM_IDENTIFIERS,
                ELM_COMMENTS,
                tokenSetOf(STRING_LITERAL),
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
            is ElmOperatorDeclarationLeft -> "Operator Declaration"
            is ElmTypeAliasDeclaration -> "Type Alias"
            is ElmTypeDeclaration -> "Union Type"
            is ElmUnionMember -> "Union Member"
            is ElmLowerPattern -> "Value Binding"
            is ElmPatternAs -> "Destructured Pattern Alias"
            is ElmPortAnnotation -> "Port Annotation"
            else -> "unknown type for ${element}"
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