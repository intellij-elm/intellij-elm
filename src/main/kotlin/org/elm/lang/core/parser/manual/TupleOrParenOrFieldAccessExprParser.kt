package org.elm.lang.core.parser.manual

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.*
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

class TupleOrParenOrFieldAccessExprParser(val exprParser: Parser) : Parser {

    override fun parse(b: PsiBuilder, level: Int): Boolean {
        val tupleOrParens: PsiBuilder.Marker = enter_section_(b)

        if (!consumeTokenSmart(b, LEFT_PARENTHESIS)) {
            exit_section_(b, tupleOrParens, null, false)
            return false
        }

        if (!exprParser.parse(b, level)) {
            exit_section_(b, tupleOrParens, null, false)
            return false
        }

        if (consumeTokenFast(b, RIGHT_PARENTHESIS)) {
            exit_section_(b, tupleOrParens, PARENTHESIZED_EXPR, true)
            return true
        }

        // parse: comma followed by expression

        if (!consumeTokenFast(b, COMMA)) {
            exit_section_(b, tupleOrParens, null, false)
            return false
        }

        if (!exprParser.parse(b, level)) {
            exit_section_(b, tupleOrParens, null, false)
            return false
        }


        // parse any remaining commas followed by expressions until closing parenthesis

        while (b.tokenType != RIGHT_PARENTHESIS) {
            // parse: comma followed by expression

            if (!consumeTokenFast(b, COMMA)) {
                exit_section_(b, tupleOrParens, null, false)
                return false
            }

            if (!exprParser.parse(b, level)) {
                exit_section_(b, tupleOrParens, null, false)
                return false
            }
        }

        if (!consumeTokenFast(b, RIGHT_PARENTHESIS)) {
            exit_section_(b, tupleOrParens, null, false)
            return false
        }

        exit_section_(b, tupleOrParens, TUPLE_EXPR, true)
        return true
    }
}
