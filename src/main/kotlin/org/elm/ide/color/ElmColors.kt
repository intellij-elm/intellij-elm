package org.elm.ide.color

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

enum class ElmColor(humanName: String, default: TextAttributesKey) {

    KEYWORD("Keyword", Default.KEYWORD),
    BAD_CHAR("Bad Character", HighlighterColors.BAD_CHARACTER),
    COMMENT("Comment", Default.LINE_COMMENT),
    STRING("String//String text", Default.STRING),
    VALID_STRING_ESCAPE("String//Valid escape sequence", Default.VALID_STRING_ESCAPE),
    INVALID_STRING_ESCAPE("String//Invalid escape sequence", Default.INVALID_STRING_ESCAPE),
    NUMBER("Number", Default.NUMBER),
    DOT("Punctuation//Dot", Default.DOT),
    ARROW("Punctuation//Arrows", Default.OPERATION_SIGN),
    OPERATOR("Operator", Default.OPERATION_SIGN),
    PARENTHESIS("Punctuation//Parentheses", Default.PARENTHESES),
    BRACES("Punctuation//Braces", Default.BRACES),
    BRACKETS("Punctuation//Brackets", Default.BRACKETS),
    COMMA("Punctuation//Comma", Default.COMMA),
    EQ("Punctuation//Equals", Default.OPERATION_SIGN),
    PIPE("Punctuation//Pipe", Default.OPERATION_SIGN),

    /**
     * The name of a definition.
     *
     * e.g. 'foo' in 'foo x y = x * y'
     */
    DEFINITION_NAME("Definition Name", Default.FUNCTION_DECLARATION),

    /**
     * The lowercase identifier name on the left-hand side of a type annotation.
     *
     * e.g. 'foo' in 'foo : String -> Cmd msg'
     */
    TYPE_ANNOTATION_NAME("Type Annotation Name", Default.FUNCTION_DECLARATION),

    /**
     * The uppercase identifier for a union (custom type) variant
     */
    UNION_VARIANT("Custom Type Variant", Default.IDENTIFIER),

    /**
     * Type expressions
     *
     * e.g. 'String' and 'Cmd msg' in 'foo : String -> Cmd msg'
     */
    TYPE_EXPR("Type", Default.CLASS_REFERENCE),

    RECORD_FIELD("Records//Field", Default.INSTANCE_FIELD),
    RECORD_FIELD_ACCESSOR("Records//Field Accessor", Default.STATIC_FIELD);


    val textAttributesKey = TextAttributesKey.createTextAttributesKey("org.elm.$name", default)
    val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
}
