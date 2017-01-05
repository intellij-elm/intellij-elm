package org.elm.lang.core.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements.MAY
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.lexer.ElmLexer
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.parser.manual.ElmManualPsiElementFactory


class ElmParserDefinition : ParserDefinition {

    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS = TokenSet.create(ElmTypes.COMMENT)
        val STRINGS = TokenSet.create(ElmTypes.STRING_LITERAL)
        val FILE = IFileElementType(Language.findInstance(ElmLanguage.javaClass))
    }

    override fun createLexer(project: Project?)
            = ElmLexer()

    override fun getWhitespaceTokens()
            = WHITE_SPACES

    override fun getCommentTokens()
            = COMMENTS

    override fun getStringLiteralElements()
            = STRINGS

    override fun createParser(project: Project?)
            = ElmParser()

    override fun getFileNodeType()
            = FILE

    override fun createFile(viewProvider: FileViewProvider?)
            = ElmFile(viewProvider!!)

    override fun spaceExistanceTypeBetweenTokens(left: ASTNode?, right: ASTNode?)
            = MAY // TODO [kl] re-visit this later

    override fun createElement(node: ASTNode?)
            = ElmManualPsiElementFactory.createElement(node)
            ?: ElmTypes.Factory.createElement(node)
}