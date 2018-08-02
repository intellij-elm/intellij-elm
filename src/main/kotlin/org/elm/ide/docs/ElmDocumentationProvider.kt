package org.elm.ide.docs

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmTypes.*
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.prevSiblings
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope
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
        is ElmTypeAliasDeclaration -> documentationFor(element)
        else -> null
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement? {
        if (context !is ElmPsiElement) return null
        val lastDot = link.indexOfLast { it == '.' }
        return if (lastDot <= 0) {
            ModuleScope(context.elmFile).getVisibleTypes().find { it.name == link }
        } else {
            val qualifierPrefix = link.substring(0, lastDot)
            val name = link.substring(lastDot + 1)
            val imports = ImportScope.fromQualifierPrefixInModule(qualifierPrefix, context.elmFile)
            imports?.getExposedTypes()?.find { it.name == name }
        }
    }
}

private fun documentationFor(decl: ElmFunctionDeclarationLeft): String? = buildString {
    val prev = decl.parent.skipWsAndVirtDeclsBackwards()
    when (prev) {
        is ElmTypeAnnotation -> {
            val id = (prev.lowerCaseIdentifier ?: prev.operatorIdentifier) ?: return null
            val typeRef = prev.typeRef ?: return null
            definition {
                b { append(id.text) }
                append(" : ")
                appendDefinition(typeRef)
                append("\n")
                b { append(decl.lowerCaseIdentifier.text) }
                for (pat in decl.patternList) {
                    append(" ").append(pat.text)
                }
            }
            prev.skipWsAndVirtDeclsBackwards()?.let {
                renderDocContent(it)
            }
        }
        else -> {
            definition {
                append(decl.text.escaped)
            }
            renderDocContent(prev)
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

private fun documentationFor(decl: ElmTypeAliasDeclaration): String? = buildString {
    val name = decl.nameIdentifier
    val types = decl.lowerTypeNameList

    definition {
        b { append("type alias") }
        append(" ").append(name.text)
        for (type in types) {
            append(" ").append(type.name)
        }
    }

    renderDocContent(decl.skipWsAndVirtDeclsBackwards())

    val record = decl.aliasedRecord
    if (record != null) {
        val recordTypes = record.fieldTypeList
        if (recordTypes.isNotEmpty()) {
            sections {
                section("Fields") {
                    append("<p>")
                    for (type in recordTypes) {
                        append("<p><code>${type.lowerCaseIdentifier.text}</code> : ")
                        appendDefinition(type.typeRef)
                    }
                }
            }
        }
    }
}

private fun StringBuilder.appendDefinition(ref: ElmUpperPathTypeRef) {
    val refText = ref.upperCaseQID.upperCaseIdentifierList.joinToString(".") { it.text }
    createLink(this, refText, ref.text)
}

private fun StringBuilder.appendDefinition(ref: ElmTypeVariableRef) {
    append(ref.identifier.text)
}

private fun StringBuilder.appendDefinition(record: ElmRecordType) {
    append("{ ")
    for ((i, field) in record.fieldTypeList.withIndex()) {
        if (i > 0) append(", ")
        append(field.lowerCaseIdentifier.text).append(" : ")
        appendDefinition(field.typeRef)
    }
    append(" }")
}

private fun StringBuilder.appendDefinition(tuple: ElmTupleType) {
    val unit = tuple.unit
    if (unit == null) {
        append("( ")
        for ((i, ref) in tuple.typeRefList.withIndex()) {
            if (i > 0) append(", ")
            appendDefinition(ref)
        }
        append(" )")
    } else {
        append("()")
    }
}

private fun StringBuilder.appendDefinition(ref: ElmParametricTypeRef) {
    val qid = ref.upperCaseQID
    createLink(this, qid.text, qid.text)
    for (param in ref.allParameters) {
        append(" ")
        when (param) {
            is ElmUpperPathTypeRef -> {
                appendDefinition(param)
            }
            is ElmRecordType -> {
                appendDefinition(param)
            }
            is ElmTupleType -> {
                appendDefinition(param)
            }
            is ElmTypeVariableRef -> {
                appendDefinition(param)
            }
            is ElmTypeRef -> {
                appendDefinition(param)
            }
        }
    }
}

private fun StringBuilder.appendDefinition(ref: ElmTypeRef) {
    for ((i, param) in ref.allParameters.withIndex()) {
        if (i > 0) append(" -> ".escaped)
        when (param) {
            is ElmUpperPathTypeRef -> {
                appendDefinition(param)
            }
            is ElmRecordType -> {
                appendDefinition(param)
            }
            is ElmTupleType -> {
                appendDefinition(param)
            }
            is ElmParametricTypeRef -> {
                appendDefinition(param)
            }
            is ElmTypeVariableRef -> {
                appendDefinition(param)
            }
            is ElmTypeRef -> {
                append("(")
                appendDefinition(param)
                append(")")
            }
        }
    }
}

private fun createLink(buffer: StringBuilder, refText: String, text: String) {
    DocumentationManagerUtil.createHyperlink(buffer, refText, text, true)
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

private inline fun StringBuilder.sections(block: StringBuilder.() -> Unit) {
    append(DocumentationMarkup.SECTIONS_START)
    block()
    append(DocumentationMarkup.SECTIONS_END)
}

private inline fun StringBuilder.section(title: String, block: StringBuilder.() -> Unit) {
    append(DocumentationMarkup.SECTION_HEADER_START, title, ":", DocumentationMarkup.SECTION_SEPARATOR)
    block()
    append(DocumentationMarkup.SECTION_END)
}

private val String.escaped: String get() = StringUtil.escapeXml(this)
