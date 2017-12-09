package org.elm.lang.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.elm.lang.core.lexer.ElmIncrementalLexer
import org.elm.lang.core.psi.ELM_IDENTIFIERS
import org.elm.lang.core.psi.ELM_KEYWORDS


class ElmNamesValidator : NamesValidator {


    override fun isKeyword(name: String, project: Project?) =
            getLexerType(name) in ELM_KEYWORDS


    override fun isIdentifier(name: String, project: Project?) =
            getLexerType(name) in ELM_IDENTIFIERS
}


private fun getLexerType(text: String): IElementType? {
    val lexer = ElmIncrementalLexer()
    lexer.start(text)
    return if (lexer.tokenEnd == text.length)
        lexer.tokenType
    else
        null
}
