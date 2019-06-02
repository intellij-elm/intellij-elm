package org.elm.ide.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.elm.lang.core.lexer.ElmIncrementalLexer
import org.elm.lang.core.psi.ELM_IDENTIFIERS
import org.elm.lang.core.psi.ELM_KEYWORDS
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.ElmTypes.OPERATOR_IDENTIFIER


class ElmNamesValidator : NamesValidator {


    override fun isKeyword(name: String, project: Project?) =
            getLexerType(name) in ELM_KEYWORDS


    override fun isIdentifier(name: String, project: Project?): Boolean {
        // TODO [kl] eventually we will want to restrict this based on context
        return when (getLexerType(name)) {
            in ELM_IDENTIFIERS -> true
            OPERATOR_IDENTIFIER -> true
            else -> false
        }
    }
}


fun isValidLowerIdentifier(text: String) =
        getLexerType(text) == ElmTypes.LOWER_CASE_IDENTIFIER


fun isValidUpperIdentifier(text: String) =
        getLexerType(text) == ElmTypes.UPPER_CASE_IDENTIFIER


private fun getLexerType(text: String): IElementType? {
    val lexer = ElmIncrementalLexer()
    lexer.start(text)
    return if (lexer.tokenEnd == text.length)
        lexer.tokenType
    else
        null
}
