package org.elm.lang.core.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements.MAY
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.lexer.ElmLexer
import org.elm.lang.core.parser.manual.ElmManualPsiElementFactory
import org.elm.lang.core.psi.ELM_COMMENTS
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmTypes


class ElmParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(ElmLanguage)
    }

    override fun createLexer(project: Project?) =
            ElmLexer()

    override fun getWhitespaceTokens() =
            TokenSet.create(TokenType.WHITE_SPACE)

    override fun getCommentTokens() =
            ELM_COMMENTS

    override fun getStringLiteralElements() =
            TokenSet.create(ElmTypes.STRING_LITERAL)

    override fun createParser(project: Project?) =
            ElmParser()

    override fun getFileNodeType() =
            FILE

    override fun createFile(viewProvider: FileViewProvider?) =
            ElmFile(viewProvider!!)

    override fun spaceExistanceTypeBetweenTokens(left: ASTNode?, right: ASTNode?) =
            MAY

    override fun createElement(node: ASTNode?) =
            ElmManualPsiElementFactory.createElement(node)
                    ?: ElmTypes.Factory.createElement(node)
}