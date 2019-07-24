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
                    "sig_left" to ElmColor.TYPE_ANNOTATION_NAME,
                    "sig_right" to ElmColor.TYPE_ANNOTATION_SIGNATURE_TYPES,
                    "type" to ElmColor.TYPE,
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

type alias <type>Model</type> =
    { <field>page</field> : <type>Int</type>
    , <field>title</field> : <type>String</type>
    , <field>stepper</field> : <type>Int</type> -> <type>Int</type>
    }

type <type>Msg</type>
    = ModeA
    | ModeB <type>Int</type>

<sig_left>update</sig_left> : <sig_right>Msg</sig_right> -> <sig_right>Model</sig_right> -> ( <sig_right>Model</sig_right>, <sig_right>Cmd Msg</sig_right> )
<func_decl>update</func_decl> msg model =
    case msg of
        ModeA ->
            { model
                | <field>page</field> = 0
                , <field>title</field> = "Mode A"
                , <field>stepper</field> = (\k -> k + 1)
            }
                ! []

<sig_left>view</sig_left> : <sig_right>Model</sig_right> -> <sig_right>Html.Html Msg</sig_right>
<func_decl>view</func_decl> model =
    let
        itemify label =
            li [] [ text label ]
    in
        div []
            [ h1 [] [ text "Chapter One" ]
            , ul []
                (List.map <accessor>.value</accessor> model.<field>items</field>)
            ]
"""
