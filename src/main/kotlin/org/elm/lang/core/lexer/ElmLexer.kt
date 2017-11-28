package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.text.CharArrayCharSequence
import org.elm.lang.core.psi.ELM_COMMENTS
import org.elm.lang.core.psi.ElmTypes.*
import java.util.*


/**
 * Ported from HaskellLayoutLexer in https://github.com/alexanderkiel/idea-haskell
 */
class ElmLexer(private val lexer: Lexer) : LexerBase() {

    companion object {
        private val CONTEXT_CREATING_KEYWORDS = TokenSet.create(LET, OF)
        private val WHITE_SPACE_TOKENS = TokenSet.orSet(TokenSet.create(TokenType.WHITE_SPACE, TAB), ELM_COMMENTS)
        private val TAB_STOP_GAP = 8
    }

    private var state: State? = null
    private var pendingToken: Token? = null
    private val indentStack = LinkedList<Int>()
    private var currentToken: Token? = null
    private var firstColumnOffset = 0
    private var additionalTabIndent = 0
    private var beginOfLine = false


    private enum class State {
        /** Start state: emits virtual tokens when offside rule matches */
        NORMAL,

        /** waiting for an appropriate token to establish a new section indent level */
        OPEN_SECTION,

        /** a token has been buffered and should be returned next */
        RETURN_PENDING
    }

    //---------------------------------------------------------------------------------------------
    // Lexer Implementation
    //---------------------------------------------------------------------------------------------

    @Deprecated("")
    fun start(buffer: CharArray, startOffset: Int, endOffset: Int, initialState: Int) {
        val arrayCharSequence = CharArrayCharSequence(*buffer)
        start(arrayCharSequence, startOffset, endOffset, initialState)
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        if (startOffset != 0) {
            throw IllegalArgumentException("do not support incremental lexing: startOffset == 0")
        }
        if (initialState != 0) {
            throw IllegalArgumentException("do not support incremental lexing: initialState == 0")
        }

        // Reset own state
        state = State.NORMAL
        indentStack.clear()
        indentStack.add(1) // top-level layout context is the first character of each line
        firstColumnOffset = 0
        additionalTabIndent = 0
        beginOfLine = true

        // Start the incremental lexer
        lexer.start(buffer, startOffset, endOffset, initialState)

        // Advance one step in order to determine our first token
        advance()
    }

    override fun getState(): Int {
        // NOTE: the underlying lexer state does not fully capture the state of this object,
        // but as long as we are never used as an incremental lexer (i.e. syntax highlighter),
        // then this should be ok.
        return lexer.state
    }

    override fun getTokenType(): IElementType? =
            currentToken!!.tokenType

    override fun getTokenStart() =
            currentToken!!.tokenStart

    override fun getTokenEnd() =
            currentToken!!.tokenEnd

    override fun advance() {
        var nextToken: Token? = null
        var advance = true

        val token: Token
        if (state == State.RETURN_PENDING) {
            token = pendingToken ?: throw IllegalStateException("no pending token to return")
            pendingToken = null
            state = State.NORMAL
            advance = false
        } else {
            token = getTokenFromLexer()
        }

        if (token.tokenType == null) {

            // End-of-File: Close all open contexts
            if (!indentStack.isEmpty()) {
                indentStack.pop()
                // TODO [kl] why did I have to put in this hack to override tokenStart/tokenEnd?
                // at this point, currentToken was the token BEFORE the EOF, and I had to make
                // it so that the synthetic token uses the token just read from the lexer instead
                // because currentToken isn't set until the end of this function. It seems like
                // it would always be broken, but it only appears to be broken in the EOF case.
                nextToken = synthesizeEndSectionToken(token.tokenStart, token.tokenEnd)
            } else {
                nextToken = token
            }

            advance = false

        } else if (state == State.NORMAL) {

            // TODO [kl] I also had to add a null check here for currentToken.
            // Maybe the original code handled EOF in strange ways (or it has changed in
            // later versions of IntelliJ?)
            if (beginOfLine && !WHITE_SPACE_TOKENS.contains(token.tokenType) && currentToken != null) {
                val indent = getIndent(token)
                if (!indentStack.isEmpty()) {
                    if (indentStack.peek() == indent) {
                        pendingToken = token
                        nextToken = synthesizeEndDeclarationToken()
                        state = State.RETURN_PENDING
                    } else if (indentStack.peek() > indent) {
                        indentStack.pop()
                        pendingToken = token
                        nextToken = synthesizeEndSectionToken()
                        state = State.RETURN_PENDING
                    }
                }
            } else if (!beginOfLine && token.tokenType == IN && currentToken?.tokenType != VIRTUAL_END_SECTION) {
                // Reached the end of let declarations where the 'in' keyword is on the same line
                // as the last 'let' declaration. Close out the section.
                indentStack.pop()
                pendingToken = token
                nextToken = synthesizeEndSectionToken()
                state = State.RETURN_PENDING
            }

            if (nextToken == null) {
                if (CONTEXT_CREATING_KEYWORDS.contains(token.tokenType)) {
                    // We have to open a new context here
                    // Output the current token and go into the OPEN_SECTION state
                    nextToken = token
                    state = State.OPEN_SECTION
                } else {
                    nextToken = token
                }
            }

        } else if (state == State.OPEN_SECTION) {

            if (WHITE_SPACE_TOKENS.contains(token.tokenType)) {
                // Skip over white spaces
                nextToken = token
            } else if (token.tokenType == null) {
                // EOF
                nextToken = token
                state = State.NORMAL
            } else {
                // Found the beginning of the section
                indentStack.push(getIndent(token))
                nextToken = token
                state = State.NORMAL
            }

        } else {
            throw IllegalStateException("unknown state")
        }

        // Unset clean line if something other than a white space or an implicit end-section is output
        if (!WHITE_SPACE_TOKENS.contains(nextToken.tokenType) && (nextToken.tokenType !== VIRTUAL_END_SECTION || nextToken.tokenEnd != nextToken.tokenStart)) {
            beginOfLine = false
        }

        currentToken = nextToken

        if (advance) {
            lexer.advance()
        }
    }

    override fun getBufferSequence() =
            lexer.bufferSequence

    override fun getBufferEnd() =
            lexer.bufferEnd

    //---------------------------------------------------------------------------------------------
    // Helper Methods
    //---------------------------------------------------------------------------------------------

    private fun getTokenFromLexer(): Token {
        val tokenType = lexer.tokenType
        val tokenStart = lexer.tokenStart
        val tokenEnd = lexer.tokenEnd

        if (tokenType === NEWLINE) {
            firstColumnOffset = tokenEnd
            additionalTabIndent = 0
            beginOfLine = true
            return Token(TokenType.WHITE_SPACE, tokenStart, tokenEnd)
        } else if (beginOfLine && tokenType === TAB) {
            // We have tabs at the beginning of line.
            // Lets correct the indent calculation
            // Increment the additional indent which comes from tabs.
            val token = Token(tokenType, tokenStart, tokenEnd)
            additionalTabIndent += TAB_STOP_GAP - (getIndent(token) - 1) % TAB_STOP_GAP - 1
            return token
        } else {
            return Token(tokenType, tokenStart, tokenEnd)
        }
    }

    private fun synthesizeEndSectionToken() =
            synthesizeEndSectionToken(currentToken!!.tokenStart, currentToken!!.tokenStart)

    private fun synthesizeEndSectionToken(tokenStart: Int, tokenEnd: Int) =
            Token(VIRTUAL_END_SECTION, tokenStart, tokenEnd)

    private fun synthesizeEndDeclarationToken() =
            Token(VIRTUAL_END_DECL, currentToken!!.tokenStart, currentToken!!.tokenStart)

    private fun getIndent(token: Token) =
            token.tokenStart - firstColumnOffset + 1 + additionalTabIndent
}


private data class Token(val tokenType: IElementType?, val tokenStart: Int, val tokenEnd: Int) {
    override fun toString() =
            "${tokenType.toString()} ($tokenStart, $tokenEnd)"
}