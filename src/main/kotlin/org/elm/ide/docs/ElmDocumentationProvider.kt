package org.elm.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.prevSiblings
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.SimpleTagProvider
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import java.net.URI

class ElmDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?) = when (element) {
        is ElmFunctionDeclarationLeft -> documentationFor(element)
        is ElmTypeDeclaration -> documentationFor(element)
        else -> null
    }
}

private fun documentationFor(decl: ElmFunctionDeclarationLeft) = buildString {
    val prev = decl.parent.skipWsAndVirtDeclsBackwards()
    when (prev?.elementType) {
        TYPE_ANNOTATION -> {
            definition {
                append(prev!!.text.escaped)
                append("\n")
                append(decl.text.escaped)
            }
            prev!!.skipWsAndVirtDeclsBackwards()?.let {
                renderDocContent(it)
            }
        }
        BLOCK_COMMENT -> {
            definition {
                append(decl.text.escaped)
            }
            renderDocContent(prev)
        }
        else -> {
            definition {
                append(decl.text.escaped)
            }
        }
    }
}

private fun documentationFor(decl: ElmTypeDeclaration): String? = buildString {
    val name = decl.nameIdentifier
    val types = decl.lowerTypeNameList

    definition {
        b { append("type") }
        append(" ").append(name.text)
        for (type in types) {
            append(" ").append(type.name)
        }
    }

    renderDocContent(decl.skipWsAndVirtDeclsBackwards())
}

private fun PsiElement.skipWsAndVirtDeclsBackwards(): PsiElement? = prevSiblings.firstOrNull {
    it !is PsiWhiteSpace && it.elementType != VIRTUAL_END_DECL
}

private fun StringBuilder.renderDocContent(element: PsiElement?) {
    if (element == null || element.elementType != BLOCK_COMMENT || !element.text.startsWith("{-|")) return

    // strip the comment markers
    val content = element.text?.let { text ->
        val i = text.indexOf("{-|")
        val j = text.lastIndexOf("-}")
        text.substring(i + 3, j)
    } ?: return

    val flavor = ElmDocMarkdownFlavourDescriptor()
    val root = MarkdownParser(flavor).buildMarkdownTreeFromString(content)
    content {
        append(HtmlGenerator(content, root, flavor).generateHtml())
    }
}

private class ElmDocMarkdownFlavourDescriptor(
        private val gfm: MarkdownFlavourDescriptor = GFMFlavourDescriptor()
) : MarkdownFlavourDescriptor by gfm {

    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
        val generatingProviders = HashMap(gfm.createHtmlGeneratingProviders(linkMap, baseURI))
        // Filter out MARKDOWN_FILE to avoid producing unnecessary <body> tags
        generatingProviders.remove(MarkdownElementTypes.MARKDOWN_FILE)
        // h1 and h2 are too large
        generatingProviders[MarkdownElementTypes.ATX_1] = SimpleTagProvider("h2")
        generatingProviders[MarkdownElementTypes.ATX_2] = SimpleTagProvider("h3")
        return generatingProviders
    }
}

private inline fun StringBuilder.definition(block: () -> Unit) {
    append(DocumentationMarkup.DEFINITION_START)
    block()
    append(DocumentationMarkup.DEFINITION_END)
}

private inline fun StringBuilder.content(block: () -> Unit) {
    append("\n") // just for html readability
    append(DocumentationMarkup.CONTENT_START)
    block()
    append(DocumentationMarkup.CONTENT_END)
}

private inline fun StringBuilder.b(block: () -> Unit) {
    append("<b>")
    block()
    append("</b>")
}

private val String.escaped: String get() = StringUtil.escapeXml(this)
