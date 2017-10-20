package org.elm.lang.core.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.text.CharArrayCharSequence;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;

import static org.elm.lang.core.psi.ElmTypes.COMMENT;
import static org.elm.lang.core.psi.ElmTypes.DOC_COMMENT;
import static org.elm.lang.core.psi.ElmTypes.LET;
import static org.elm.lang.core.psi.ElmTypes.LINE_COMMENT;
import static org.elm.lang.core.psi.ElmTypes.OF;
import static org.elm.lang.core.psi.ElmTypes.NEWLINE;
import static org.elm.lang.core.psi.ElmTypes.TAB;
import static org.elm.lang.core.psi.ElmTypes.VIRTUAL_END_DECL;
import static org.elm.lang.core.psi.ElmTypes.VIRTUAL_END_SECTION;


/**
 * @author Alexander Kiel
 *
 * Based on HaskellLayoutLexer from https://github.com/alexanderkiel/idea-haskell
 */
public class ElmLexer extends LexerBase {

    private final static TokenSet CONTEXT_CREATING_KEYWORDS = TokenSet.create(LET, OF);

    private final static TokenSet COMMENT_TOKENS = TokenSet.create(COMMENT, DOC_COMMENT, LINE_COMMENT);
    private final static TokenSet WHITE_SPACE_TOKENS = TokenSet.orSet(TokenSet.create(TokenType.WHITE_SPACE, TAB), COMMENT_TOKENS);

    private final static int TAB_STOP_GAP = 8;

    private final Lexer lexer;

    private enum State {
        NORMAL, OPEN_SECTION, RETURN_PENDING
    }

    private State state;

    private Token pendingToken;
    private final Deque<Integer> indentStack = new LinkedList<Integer>();

    private Token currentToken;

    private int firstColumnOffset;
    private int additionalTabIndent;
    private boolean beginOfLine;

    //---------------------------------------------------------------------------------------------
    // Constructor
    //---------------------------------------------------------------------------------------------

    public ElmLexer(@NotNull Lexer lexer) {
        this.lexer = lexer;
    }

    //---------------------------------------------------------------------------------------------
    // Lexer Implementation
    //---------------------------------------------------------------------------------------------

    @Deprecated
    public void start(char[] buffer, int startOffset, int endOffset, int initialState) {
        final CharArrayCharSequence arrayCharSequence = new CharArrayCharSequence(buffer);
        start(arrayCharSequence, startOffset, endOffset, initialState);
    }

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        if (startOffset != 0) {
            throw new IllegalArgumentException("do not support incremental lexing: startOffset == 0");
        }
        if (initialState != 0) {
            throw new IllegalArgumentException("do not support incremental lexing: initialState == 0");
        }

        // Reset own state
        state = State.NORMAL;
        indentStack.clear();
        indentStack.add(1); // top-level layout context is the first character of each line
        firstColumnOffset = 0;
        additionalTabIndent = 0;
        beginOfLine = true;

        // Start the incremental lexer
        lexer.start(buffer, startOffset, endOffset, initialState);

        // Advance one step in order to determine our first token
        advance();
    }

    public int getState() {
        // NOTE: the underlying lexer state does not fully capture the state of this object,
        // but as long as we are never used as an incremental lexer (i.e. syntax highlighter),
        // then this should be ok.
        return lexer.getState();
    }

    public IElementType getTokenType() {
        return currentToken.tokenType;
    }

    public int getTokenStart() {
        return currentToken.tokenStart;
    }

    public int getTokenEnd() {
        return currentToken.tokenEnd;
    }

    public void advance() {
        Token nextToken = null;
        boolean advance = true;

        final Token token;
        if (state == State.RETURN_PENDING) {
            if (pendingToken == null) {
                throw new IllegalStateException("no pending token to return");
            }

            token = pendingToken;
            pendingToken = null;
            state = State.NORMAL;
            advance = false;
        } else {
            token = getTokenFromLexer();
        }

        if (token.tokenType == null) {

            // End-of-File: Close all open contexts
            if (!indentStack.isEmpty()) {
                indentStack.pop();
                // TODO [kl] why did I have to put in this hack to override tokenStart/tokenEnd?
                // at this point, currentToken was the token BEFORE the EOF, and I had to make
                // it so that the synthetic token uses the token just read from the lexer instead
                // because currentToken isn't set until the end of this function. It seems like
                // it would always be broken, but it only appears to be broken in the EOF case.
                nextToken = synthesizeEndSectionToken(token.tokenStart, token.tokenEnd);
            } else {
                nextToken = token;
            }

            advance = false;

        } else if (state == State.NORMAL) {

            // TODO [kl] why did I have to add the null check on currentToken?
            if (beginOfLine && !WHITE_SPACE_TOKENS.contains(token.tokenType) && currentToken != null) {

                int indent = getIndent(token);
                if (!indentStack.isEmpty()) {
                    if (indentStack.peek() == indent) {

                        pendingToken = token;
                        nextToken = synthesizeEndDeclarationToken();
                        state = State.RETURN_PENDING;

                    } else if (indentStack.peek() > indent) {

                        indentStack.pop();
                        pendingToken = token;
                        nextToken = synthesizeEndSectionToken();
                        state = State.RETURN_PENDING;

                    }
                }

            }

            if (nextToken == null) {
                if (CONTEXT_CREATING_KEYWORDS.contains(token.tokenType)) {

                    // We have to open a new context here
                    // Output the current token and go into the OPEN_SECTION state
                    nextToken = token;
                    state = State.OPEN_SECTION;

                } else {
                    nextToken = token;
                }
            }

        } else if (state == State.OPEN_SECTION) {

            if (WHITE_SPACE_TOKENS.contains(token.tokenType)) {

                // Skip over white spaces
                nextToken = token;

            } else if (token.tokenType == null) {

                // EOF
                nextToken = token;
                state = State.NORMAL;

            } else {

                // Found the beginning of the section
                indentStack.push(getIndent(token));
                nextToken = token;
                state = State.NORMAL;
            }

        } else {
            throw new IllegalStateException("unknown state");
        }

        // Unset clean line if something other than a white space or an implicit end-section is output
        if (!WHITE_SPACE_TOKENS.contains(nextToken.tokenType) && (nextToken.tokenType != VIRTUAL_END_SECTION ||
                nextToken.tokenEnd != nextToken.tokenStart)) {
            beginOfLine = false;
        }

        currentToken = nextToken;

        if (advance) {
            lexer.advance();
        }
    }

    @Deprecated
    public char[] getBuffer() {
        return CharArrayUtil.fromSequence(getBufferSequence());
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return lexer.getBufferSequence();
    }

    public int getBufferEnd() {
        return lexer.getBufferEnd();
    }

    //---------------------------------------------------------------------------------------------
    // Helper Methods
    //---------------------------------------------------------------------------------------------

    @NotNull
    private Token getTokenFromLexer() {
        IElementType tokenType = lexer.getTokenType();
        int tokenStart = lexer.getTokenStart();
        int tokenEnd = lexer.getTokenEnd();

        if (tokenType == NEWLINE) {
            firstColumnOffset = tokenEnd;
            additionalTabIndent = 0;
            beginOfLine = true;
            return new Token(TokenType.WHITE_SPACE, tokenStart, tokenEnd);
        } else if (beginOfLine && tokenType == TAB) {

            // We have tabs at the beginning of line.
            // Lets correct the indent calculation
            Token token = new Token(tokenType, tokenStart, tokenEnd);

            // Increment the additional indent which comes from tabs.
            additionalTabIndent += TAB_STOP_GAP - ((getIndent(token) - 1) % TAB_STOP_GAP) - 1;

            return token;
        } else {
            return new Token(tokenType, tokenStart, tokenEnd);
        }
    }

    @NotNull
    private Token synthesizeEndSectionToken() {
        return synthesizeEndSectionToken(currentToken.tokenStart, currentToken.tokenStart);
    }

    private Token synthesizeEndSectionToken(int tokenStart, int tokenEnd) {
        return new Token(VIRTUAL_END_SECTION, tokenStart, tokenEnd);
    }

    @NotNull
    private Token synthesizeEndDeclarationToken() {
        return new Token(VIRTUAL_END_DECL, currentToken.tokenStart, currentToken.tokenStart);
    }

    private int getIndent(Token token) {
        return token.tokenStart - firstColumnOffset + 1 + additionalTabIndent;
    }
}

/**
 * @author Alexander Kiel
 * @version $Id$
 */
class Token {

    final IElementType tokenType;
    final int tokenStart;
    final int tokenEnd;

    Token(IElementType tokenType, int tokenStart, int tokenEnd) {
        this.tokenType = tokenType;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
    }

    @Override
    public String toString() {
        return tokenType + " (" + tokenStart + ", " + tokenEnd + ")";
    }
}