package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.*
import com.intellij.psi.tree.IElementType
import org.elm.lang.core.psi.ElmTypes.*

/*
Ideally we could define something like this in GrammarKit, but it doesn't parse
the way we want due to some weird interaction with GrammarKit's left-recursive
expression parser system (extends/collapse).

TupleOrParenOrFieldAccessExpr ::=
    LEFT_PARENTHESIS Expression (FieldAccessUpper | ParenUpper | TupleUpper)
    { pin = 1 }

upper FieldAccessUpper ::=
    RIGHT_PARENTHESIS FieldAccessSegment+
    { elementType = FieldAccessExpr }

upper ParenUpper ::=
    RIGHT_PARENTHESIS
    { elementType = ParenthesizedExpr }

upper TupleUpper ::=
    (COMMA Expression)+ RIGHT_PARENTHESIS
    { elementType = TupleExpr }

 */
// TODO expand parser to also parse `FieldAccess` expressions that start with a parenthesized expr
class TupleOrParenExprParser(
        private val exprParser: Parser
) : Parser {

    override fun parse(b: PsiBuilder, level: Int): Boolean {
        val tupleOrParens: PsiBuilder.Marker = enter_section_(b, level, _NONE_)

        if (!consumeTokenSmart(b, LEFT_PARENTHESIS)) {
            exit_section_(b, level, tupleOrParens, null, false, false, null)
            return false
        }

        // Due to how our parse rules are ordered, it is safe to pin
        // as soon as we see a left parenthesis. Whether we choose to treat
        // this as a parenthesized expr or a tuple expr is arbitrary.
        // I have chosen to treat it as a paren expr until we see a comma.

        fun commit(type: IElementType, success: Boolean): Boolean {
            exit_section_(b, level, tupleOrParens, type, success, /*pinned*/ true, null)
            return true
        }

        if (!exprParser.parse(b, level)) {
            return commit(PARENTHESIZED_EXPR, success = false)
        }

        if (consumeTokenSmart(b, RIGHT_PARENTHESIS)) {
            return commit(PARENTHESIZED_EXPR, success = true)
        }

        // If this is actually a tuple expression, there should be a comma right here.

        if (!consumeToken(b, COMMA)) {
            return commit(PARENTHESIZED_EXPR, success = false)
        }

        // Now that we've seen a comma, we definitely are parsing a tuple, so
        // if there are any parse errors beyond this point, we will commit
        // it as a tuple expr (rather than a paren expr).

        if (!exprParser.parse(b, level)) {
            return commit(TUPLE_EXPR, success = false)
        }


        // parse any remaining commas followed by expressions until closing parenthesis

        while (!nextTokenIs(b, RIGHT_PARENTHESIS)) {
            // parse: comma followed by expression

            if (!consumeToken(b, COMMA)) {
                return commit(TUPLE_EXPR, success = false)
            }

            if (!exprParser.parse(b, level)) {
                return commit(TUPLE_EXPR, success = false)
            }
        }

        if (!consumeTokenSmart(b, RIGHT_PARENTHESIS)) {
            return commit(TUPLE_EXPR, success = false)
        }

        return commit(TUPLE_EXPR, success = true)
    }
}
