package org.elm.lang.core.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import static org.elm.lang.core.psi.ElmTypes.*;

%%

%{
  public _ElmLexer() {
    this((java.io.Reader)null);
  }
%}

%class _ElmLexer
%public
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
    private int commentLevel = 0;

    private void startComment() {
        commentLevel = 1;
        yybegin(IN_COMMENT);
    }
%}

%state IN_COMMENT,IN_GLSL_CODE

Newline = (\n|\r|\r\n)
Space = " "
WhiteSpace = {Space}+
Tab = \t
LineComment = ("--")[^\r\n]*
IdentifierChar = [[:letter:][:digit:]_]
HexChar = [[:digit:]A-Fa-f]
LowerCaseIdentifier = [:lowercase:]{IdentifierChar}*
UpperCaseIdentifier = [:uppercase:]{IdentifierChar}*
StringLiteral = \"(\\.|[^\\\"])*\"
StringWithQuotesLiteral = \"\"\"(\\.|[^\\\"]|\"{1,2}([^\"\\]|\\\"))*\"\"\"
NumberLiteral = ("-")?[:digit:]+(\.[:digit:]+)?
HexLiteral = 0x{HexChar}+
CharLiteral = '(\\.|\\u\{{HexChar}+\}|[^\\'])'
LegacyCharLiteral = '(\\.|\\x{HexChar}+|[^\\'])' // TODO [drop 0.18] remove this
Operator = ("!"|"$"|"^"|"|"|"*"|"/"|"?"|"+"|"~"|"."|-|=|@|#|%|&|<|>|:|€|¥|¢|£|¤)+
ReservedKeyword = ("hiding" | "export" | "foreign" | "deriving")

%%

<IN_COMMENT> {
    "{-" {
        commentLevel++;
    }
    "-}" {
            if (--commentLevel == 0) {
                yybegin(YYINITIAL);
                return BLOCK_COMMENT;
            }
        }

    <<EOF>> { commentLevel = 0; yybegin(YYINITIAL); return BLOCK_COMMENT; }

    [^] { }
}

<IN_GLSL_CODE> {
    "|]" {
        yybegin(YYINITIAL);
        return END_GLSL_CODE;
    }
    [^|]+ {
        return GLSL_CODE_CONTENT;
    }
}

<YYINITIAL> {
    "module"                    { return MODULE; }
    "where"                     { return WHERE; }
    "import"                    { return IMPORT; }
    "as"                        { return AS; }
    "exposing"                  { return EXPOSING; }
    "if"                        { return IF; }
    "then"                      { return THEN; }
    "else"                      { return ELSE; }
    "case"                      { return CASE; }
    "of"                        { return OF; }
    "let"                       { return LET; }
    "in"                        { return IN; }
    "type"                      { return TYPE; }
    "alias"                     { return ALIAS; }
    "port"                      { return PORT; }
    "infix"                     { return INFIX; }
    "infixl"                    { return INFIXL; } // TODO [drop 0.18] remove infixl entirely
    "infixr"                    { return INFIXR; } // TODO [drop 0.18] remove infixr entirely
    {ReservedKeyword}           { return RESERVED; }
    "("                         { return LEFT_PARENTHESIS; }
    ")"                         { return RIGHT_PARENTHESIS; }
    "["                         { return LEFT_SQUARE_BRACKET; }
    "]"                         { return RIGHT_SQUARE_BRACKET; }
    "{"                         { return LEFT_BRACE; }
    "}"                         { return RIGHT_BRACE; }
    ".."                        { return DOUBLE_DOT; }
    ","                         { return COMMA; }
    "="                         { return EQ; }
    "->"                        { return ARROW; }
    ":"                         { return COLON; }
    "|"                         { return PIPE; }
    "\\"                        { return BACKSLASH; }
    "_"                         { return UNDERSCORE; }
    "."                         { return DOT; }
    "[glsl|" {
        yybegin(IN_GLSL_CODE);
        return START_GLSL_CODE;
    }
    "{-" {
        startComment();
    }
    "{-|" {
        startComment();
    }
    {LineComment}               { return LINE_COMMENT; }
    {LowerCaseIdentifier}       { return LOWER_CASE_IDENTIFIER; }
    {UpperCaseIdentifier}       { return UPPER_CASE_IDENTIFIER; }
    {StringWithQuotesLiteral}   { return STRING_LITERAL; }
    {StringLiteral}             { return STRING_LITERAL; }
    {CharLiteral}               { return CHAR_LITERAL; }
    {LegacyCharLiteral}         { return CHAR_LITERAL; } // TODO [drop 0.18] remove this line
    {NumberLiteral}             { return NUMBER_LITERAL; }
    {HexLiteral}                { return NUMBER_LITERAL; }
    {Operator}                  { return OPERATOR_IDENTIFIER; }
    {WhiteSpace}                { return TokenType.WHITE_SPACE; }
    {Newline}                   { return NEWLINE; }
    {Tab}                       { return TAB; }
}

. {
    return TokenType.BAD_CHARACTER;
}
