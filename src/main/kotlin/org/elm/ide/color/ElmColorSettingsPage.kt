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
                    "variant" to ElmColor.UNION_VARIANT,
                    "accessor" to ElmColor.RECORD_FIELD_ACCESSOR,
                    "field" to ElmColor.RECORD_FIELD,
                    "func_decl" to ElmColor.DEFINITION_NAME
            ).mapValues { it.value.textAttributesKey }

    override fun getDemoText() =
            demoCodeText
}

private const val demoCodeText = """
module Todo exposing (..)

import Html exposing (div, h1, ul, li, text)

-- a single line comment

type alias Model =
    { <field>page</field> : <type>Int</type>
    , <field>title</field> : <type>String</type>
    , <field>stepper</field> : <type>Int</type> -> <type>Int</type>
    }

type Msg <type>a</type>
    = <variant>ModeA</variant>
    | <variant>ModeB</variant> <type>Maybe a</type>

<func_decl>update</func_decl> : <type>Msg</type> -> <type>Model</type> -> ( <type>Model</type>, <type>Cmd Msg</type> )
<func_decl>update</func_decl> msg model =
    case msg of
        <variant>ModeA</variant> ->
            { model
                | <field>page</field> = 0
                , <field>title</field> = "Mode A"
                , <field>stepper</field> = (\k -> k + 1)
            }
                ! []

<func_decl>view</func_decl> : <type>Model</type> -> <type>Html.Html Msg</type>
<func_decl>view</func_decl> model =
    let
        <func_decl>itemify</func_decl> label =
            li [] [ text label ]
    in
        div []
            [ h1 [] [ text "Chapter One" ]
            , ul []
                (List.map <accessor>.value</accessor> model.<field>items</field>)
            ]
"""
