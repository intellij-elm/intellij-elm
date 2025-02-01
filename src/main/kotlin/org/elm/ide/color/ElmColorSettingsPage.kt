package org.elm.ide.color

import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.elm.ide.highlight.ElmSyntaxHighlighter
import org.elm.ide.icons.ElmIcons

class ElmColorSettingsPage : ColorSettingsPage {

    private val ATTRS = ElmColor.values().map { it.attributesDescriptor }.toTypedArray()

    override fun getDisplayName() =
        "Elm"

    override fun getIcon() =
        ElmIcons.FILE

    override fun getAttributeDescriptors() =
        ATTRS

    override fun getColorDescriptors(): Array<ColorDescriptor> =
        ColorDescriptor.EMPTY_ARRAY

    override fun getHighlighter() =
        ElmSyntaxHighlighter()

    override fun getAdditionalHighlightingTagToDescriptorMap() =
        // special tags in [demoText] for semantic highlighting
        mapOf(
            "type" to ElmColor.TYPE_EXPR,
            "type_decl" to ElmColor.TYPE_DECLARATION,
            "variant" to ElmColor.UNION_VARIANT,
            "accessor" to ElmColor.RECORD_FIELD_ACCESSOR,
            "field" to ElmColor.RECORD_FIELD,
            "func_decl" to ElmColor.DEFINITION_NAME,

            "fn_arg" to ElmColor.FUNCTION_ARGUMENT,
            "fn_loc" to ElmColor.LOCAL_FUNCTION,
            "fn_loc_arg" to ElmColor.LOCAL_FUNCTION_ARGUMENT,
            "pattern_arg" to ElmColor.PATTERN_ARGUMENT,
            "fn_inl_arg" to ElmColor.INLINE_FUNCTION_ARGUMENT,

            "ext_module" to ElmColor.EXTERNAL_MODULE,
            "ext_fn_call" to ElmColor.EXTERNAL_FUNCTION_CALL,
            "port" to ElmColor.PORT,

            ).mapValues { it.value.textAttributesKey }

    override fun getDemoText() =
        demoCodeText
}

private const val demoCodeText = """
module Todo exposing (..)

import <ext_module>Html</ext_module> exposing (<ext_fn_call>div</ext_fn_call>, <ext_fn_call>h1</ext_fn_call>, <ext_fn_call>ul</ext_fn_call>, <ext_fn_call>li</ext_fn_call>, <ext_fn_call>text</ext_fn_call>)

-- a single line comment

port <port>samplePort</port> : () -> <type>Cmd msg</type>

type alias <type_decl>Model</type_decl> =
    { <field>page</field> : <type>Int</type>
    , <field>title</field> : <type>String</type>
    , <field>stepper</field> : <type>Int</type> -> <type>Int</type>
    }

type <type_decl>Msg</type_decl> <type>a</type>
    = <variant>ModeA</variant>
    | <variant>ModeB</variant> <type>Maybe a</type>

<func_decl>update</func_decl> : <type>Msg</type> -> <type>Model</type> -> ( <type>Model</type>, <type>Cmd Msg</type> )
<func_decl>update</func_decl> <fn_arg>msg</fn_arg> <fn_arg>model</fn_arg> =
    let
        <fn_loc>localFunction</fn_loc> <fn_loc_arg>a</fn_loc_arg> =
            <fn_loc_arg>a</fn_loc_arg>
    in
    case <fn_arg>msg</fn_arg> of
        <variant>ModeB</variant> <pattern_arg>maybe</pattern_arg> ->
            { <fn_arg>model</fn_arg>
                | <field>page</field> = 0
                , <field>title</field> = "Mode " ++ (<fn_loc>localFunction</fn_loc> "B")
                , <field>stepper</field> = (\<fn_inl_arg>k</fn_inl_arg> -> <fn_inl_arg>k</fn_inl_arg> + 1)
            }
                ! []

<func_decl>view</func_decl> : <type>Model</type> -> <type>Html.Html Msg</type>
<func_decl>view</func_decl> <fn_arg>model</fn_arg> =
    let
        <func_decl>itemify</func_decl> <fn_arg>label</fn_arg> =
            <ext_fn_call>li</ext_fn_call> [] [ <ext_fn_call>text</ext_fn_call> <fn_arg>label</fn_arg> ]
    in
        <ext_fn_call>div</ext_fn_call> []
            [ <ext_fn_call>h1</ext_fn_call> [] [ <ext_fn_call>text</ext_fn_call> "Chapter One" ]
            , <ext_fn_call>ul</ext_fn_call> []
                (<ext_module>List.</ext_module><ext_fn_call>map</ext_fn_call> <accessor>.value</accessor> <fn_arg>model</fn_arg>.<field>items</field>)
            ]
"""
