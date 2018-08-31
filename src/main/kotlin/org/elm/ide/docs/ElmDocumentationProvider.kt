package org.elm.ide.docs

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.ElmTypes.BLOCK_COMMENT
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.TyUnknown
import org.elm.lang.core.types.inference
import org.elm.lang.core.types.renderedText
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
        is ElmLowerPattern -> documentationFor(element)
        is ElmModuleDeclaration -> documentationFor(element)
        is ElmAsClause -> documentationFor(element)
        is ElmPatternAs -> documentationFor(element)
        else -> null
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement? {
        if (context !is ElmPsiElement) return null
        val lastDot = link.indexOfLast { it == '.' }
        return if (lastDot <= 0) {
            with(ModuleScope(context.elmFile)) {
                getVisibleTypes().find { it.name == link } ?: getDeclaredValues().find { it.name == link }
            }
        } else {
            val qualifierPrefix = link.substring(0, lastDot)
            val name = link.substring(lastDot + 1)
            ImportScope.fromQualifierPrefixInModule(qualifierPrefix, context.elmFile)
                    .flatMap { it.getExposedTypes() }
                    .find { it.name == name }
        }
    }
}

private fun documentationFor(decl: ElmFunctionDeclarationLeft): String? = buildString {
    val parent = decl.parent as? ElmValueDeclaration ?: return null
    val typeAnnotation = parent.typeAnnotation

    definition {
        if (typeAnnotation != null) {
            val id = (typeAnnotation.lowerCaseIdentifier ?: typeAnnotation.operatorIdentifier) ?: return null
            val typeRef = typeAnnotation.typeRef ?: return null
            b { append(id.text) }
            append(" : ")
            renderDefinition(typeRef)
            append("\n")
        }

        b { append(decl.lowerCaseIdentifier.text) }
        for (pat in decl.patterns) {
            append(" ")
            // As clauses can't appear at the top level, but can appear inside parentheses
            if (pat is ElmPattern && pat.patternAs != null) append("(", pat.text, ")")
            else append(pat.text)
        }

        renderDefinitionLocation(decl)
    }
    renderDocContent(parent)
}

private fun documentationFor(decl: ElmTypeDeclaration): String? = buildString {
    val name = decl.nameIdentifier
    val types = decl.lowerTypeNameList

    definition {
        b { append("type") }
        append(" ", name.text)
        for (type in types) {
            append(" ", type.name)
        }
        renderDefinitionLocation(decl)
    }

    renderDocContent(decl)

    sections {
        section("Members") {
            for (member in decl.unionMemberList) {
                append("\n<p><code>${member.upperCaseIdentifier.text}</code>")
                renderParameters(member.allParameters, " ", false, true)
            }
        }
    }
}

private fun documentationFor(decl: ElmTypeAliasDeclaration): String? = buildString {
    val name = decl.nameIdentifier
    val types = decl.lowerTypeNameList

    definition {
        b { append("type alias") }
        append(" ").append(name.text)
        for (type in types) {
            append(" ", type.name)
        }
        renderDefinitionLocation(decl)
    }

    renderDocContent(decl)

    val record = decl.aliasedRecord
    if (record != null) {
        val recordTypes = record.fieldTypeList
        if (recordTypes.isNotEmpty()) {
            sections {
                section("Fields") {
                    for (type in recordTypes) {
                        append("\n<p><code>${type.lowerCaseIdentifier.text}</code> : ")
                        renderDefinition(type.typeRef)
                    }
                }
            }
        }
    }
}

private fun documentationFor(pattern: ElmLowerPattern): String? = documentationForParameter(pattern)
private fun documentationFor(patternAs: ElmPatternAs): String? = documentationForParameter(patternAs)
private fun documentationForParameter(element: ElmNamedElement): String? = buildString {
    val function = element.parentOfType<ElmFunctionDeclarationLeft>() ?: return null
    val decl = function.parentOfType<ElmValueDeclaration>() ?: return null
    val inference = decl.inference
    val ty = inference.bindingType(element)

    definition {
        i { append("parameter") }
        append(" ", element.name, " ")

        if (ty !is TyUnknown) {
            append(": ", ty.renderedText(true), "\n")
        }

        i { append("of function ") }
        renderLink(function.name, function.name)

        renderDefinitionLocation(element)
    }
}

private fun documentationFor(decl: ElmModuleDeclaration): String? = buildString {
    val ids = decl.upperCaseQID.upperCaseIdentifierList

    definition {
        i { append("module") }
        append(" ", ids.last().text)
        if (ids.size > 1) {
            i { append(" defined in ") }
            ids.dropLast(1).joinTo(this, ".") { it.text }
        }
    }

    renderDocContent(decl) { html ->
        val declarations = ModuleScope(decl.elmFile).run { getDeclaredValues() + getDeclaredTypes() }

        // Render @docs commands
        html.replace(Regex("<p>@docs (.+?)</p>", RegexOption.DOT_MATCHES_ALL)) { match ->
            val names = match.groupValues[1].split(Regex(",\\s+"))
            names.joinToString(", ") { name ->
                val target = declarations.find { it.name == name }
                target?.let { buildString { renderLink(name, name) } } ?: name
            }
        }
    }
}

private fun documentationFor(clause: ElmAsClause): String? = buildString {
    val decl = clause.parent?.reference?.resolve() as? ElmModuleDeclaration ?: return null
    return documentationFor(decl)
}

private fun StringBuilder.renderDefinitionLocation(element: ElmPsiElement) {
    val module = element.elmFile.getModuleDecl() ?: return
    i { append(" defined in ") }
    append(module.upperCaseQID.text)
}

private fun StringBuilder.renderDefinition(ref: ElmUpperPathTypeRef) {
    renderLink(ref.upperCaseQID.text, ref.text)
}

private fun StringBuilder.renderDefinition(ref: ElmTypeVariableRef) {
    append(ref.identifier.text)
}

private fun StringBuilder.renderDefinition(record: ElmRecordType) {
    record.fieldTypeList.renderTo(this, prefix = "{ ", postfix = " }") {
        append(it.lowerCaseIdentifier.text, " : ")
        renderDefinition(it.typeRef)
    }
}

private fun StringBuilder.renderDefinition(tuple: ElmTupleType) {
    if (tuple.unit != null) append("()")
    else tuple.typeRefList.renderTo(this, prefix = "( ", postfix = " )") { renderDefinition(it) }
}

private fun StringBuilder.renderDefinition(ref: ElmParametricTypeRef) {
    val qid = ref.upperCaseQID
    renderLink(qid.text, qid.text)
    renderParameters(ref.allParameters, " ", false, false)
}

private fun StringBuilder.renderDefinition(ref: ElmTypeRef) {
    renderParameters(ref.allParameters, " -> ".escaped, true, true)
}

private fun StringBuilder.renderParameters(params: Sequence<ElmPsiElement>,
                                           sep: String,
                                           skipFirstSep: Boolean,
                                           parenthesizeTypeRefs: Boolean) {
    for ((i, param) in params.withIndex()) {
        if (i > 0 || !skipFirstSep) append(sep)
        renderParameter(param, parenthesizeTypeRefs)
    }
}

private fun StringBuilder.renderParameter(param: ElmPsiElement,
                                          parenthesizeTypeRefs: Boolean) {
    when (param) {
        is ElmUpperPathTypeRef -> renderDefinition(param)
        is ElmRecordType -> renderDefinition(param)
        is ElmTupleType -> renderDefinition(param)
        is ElmParametricTypeRef -> renderDefinition(param)
        is ElmTypeVariableRef -> renderDefinition(param)
        is ElmTypeRef -> {
            if (parenthesizeTypeRefs) append("(")
            renderDefinition(param)
            if (parenthesizeTypeRefs) append(")")
        }
    }
}

private fun StringBuilder.renderLink(refText: String, text: String) {
    DocumentationManagerUtil.createHyperlink(this, refText, text, true)
}

private fun StringBuilder.renderDocContent(element: ElmDocTarget?, transform: (String) -> String = { it }) {
    val doc = element?.docComment

    if (doc == null || doc.elementType != BLOCK_COMMENT) return

    // strip the comment markers
    val content = doc.text?.let { text ->
        val i = text.indexOf("{-|")
        val j = text.lastIndexOf("-}")
        text.substring(i + 3, j)
    } ?: return

    val flavor = ElmDocMarkdownFlavourDescriptor()
    val root = MarkdownParser(flavor).buildMarkdownTreeFromString(content)
    content {
        append(transform(HtmlGenerator(content, root, flavor).generateHtml()))
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
    append("\n", DocumentationMarkup.CONTENT_START)
    block()
    append(DocumentationMarkup.CONTENT_END)
}

private inline fun StringBuilder.b(block: () -> Unit) {
    append("<b>")
    block()
    append("</b>")
}

private inline fun StringBuilder.i(block: () -> Unit) {
    append("<i>")
    block()
    append("</i>")
}

private inline fun StringBuilder.sections(block: StringBuilder.() -> Unit) {
    append("\n", DocumentationMarkup.SECTIONS_START)
    block()
    append(DocumentationMarkup.SECTIONS_END)
}

private inline fun StringBuilder.section(title: String, block: StringBuilder.() -> Unit) {
    append(DocumentationMarkup.SECTION_HEADER_START, title, ":", DocumentationMarkup.SECTION_SEPARATOR, "<p>")
    block()
    append(DocumentationMarkup.SECTION_END)
}

private val String.escaped: String get() = StringUtil.escapeXml(this)

private fun <T, A : Appendable> Iterable<T>.renderTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", render: (A.(T) -> Unit)): A {
    buffer.append(prefix)
    for ((i, field) in withIndex()) {
        if (i > 0) buffer.append(separator)
        buffer.render(field)
    }
    buffer.append(postfix)
    return buffer
}
