package org.elm.ide.color

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

enum class ElmColor(humanName: String, default: TextAttributesKey) {

    KEYWORD("Keyword", DefaultLanguageHighlighterColors.KEYWORD),
    BAD_CHAR("Bad Character", HighlighterColors.BAD_CHARACTER),
    COMMENT("Comment", DefaultLanguageHighlighterColors.LINE_COMMENT),
    STRING("String//String text", DefaultLanguageHighlighterColors.STRING),
    VALID_STRING_ESCAPE("String//Valid escape sequence", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE),
    INVALID_STRING_ESCAPE("String//Invalid escape sequence", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE),
    NUMBER("Number", DefaultLanguageHighlighterColors.NUMBER),
    DOT("Punctuation//Dot", DefaultLanguageHighlighterColors.DOT),
    ARROW("Punctuation//Arrows", DefaultLanguageHighlighterColors.OPERATION_SIGN),
    OPERATOR("Operator", DefaultLanguageHighlighterColors.OPERATION_SIGN),
    PARENTHESIS("Punctuation//Parentheses", DefaultLanguageHighlighterColors.PARENTHESES),
    BRACES("Punctuation//Braces", DefaultLanguageHighlighterColors.BRACES),
    BRACKETS("Punctuation//Brackets", DefaultLanguageHighlighterColors.BRACKETS),
    COMMA("Punctuation//Comma", DefaultLanguageHighlighterColors.COMMA),
    EQ("Punctuation//Equals", DefaultLanguageHighlighterColors.OPERATION_SIGN),
    PIPE("Punctuation//Pipe", DefaultLanguageHighlighterColors.OPERATION_SIGN),

    /**
     * The name of a definition.
     *
     * e.g. 'foo' in 'foo x y = x * y'
     */
    DEFINITION_NAME("Definition Name", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),

    /**
     * The uppercase identifier for a type in all contexts EXCEPT when appearing
     * in a type annotation.
     */
    TYPE("Type", DefaultLanguageHighlighterColors.CLASS_NAME),

    /**
     * The lowercase identifier name in a type annotation.
     *
     * e.g. 'foo' in 'foo : String -> Cmd msg'
     */
    TYPE_ANNOTATION_NAME("Type Annotation//Name", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),

    /**
     * Both uppercase and lowercase identifiers appearing on the right-hand side
     * of a top-level type annotation.
     *
     * e.g. 'String' and 'Cmd msg' in 'foo : String -> Cmd msg'
     */
    TYPE_ANNOTATION_SIGNATURE_TYPES("Type Annotation//Signature", DefaultLanguageHighlighterColors.CLASS_REFERENCE);


    val textAttributesKey = TextAttributesKey.createTextAttributesKey("org.elm.$name", default)
    val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
}
