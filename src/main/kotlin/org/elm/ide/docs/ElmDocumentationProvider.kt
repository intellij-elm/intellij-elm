package org.elm.ide.docs

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.elm.lang.core.psi.ElmDocTarget
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.resolve.scope.QualifiedImportScope
import org.elm.lang.core.types.*
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
        is ElmUnionVariant -> documentationFor(element)
        is ElmTypeAliasDeclaration -> documentationFor(element)
        is ElmLowerPattern -> documentationFor(element)
        is ElmModuleDeclaration -> documentationFor(element)
        is ElmAsClause -> documentationFor(element)
        is ElmInfixDeclaration -> documentationFor(element)
        is ElmFieldType -> documentationFor(element)
        else -> null
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement? {
        if (context !is ElmPsiElement) return null
        val lastDot = link.indexOfLast { it == '.' }
        return if (lastDot <= 0) {
            with(ModuleScope) {
                getVisibleTypes(context.elmFile)[link]
                        ?: getDeclaredValues(context.elmFile)[link]
            }
        } else {
            val qualifierPrefix = link.substring(0, lastDot)
            val name = link.substring(lastDot + 1)
            QualifiedImportScope(qualifierPrefix, context.elmFile).getExposedType(name)
        }
    }
}

private fun documentationFor(decl: ElmFunctionDeclarationLeft): String? = buildString {
    val parent = decl.parent as? ElmValueDeclaration ?: return null
    val ty = parent.findTy()
    val id = decl.lowerCaseIdentifier.text
    definition {
        if (ty != null && ty !is TyUnknown) {
            b { append(id) }
            append(" : ")
            append(ty.renderedText(linkify = true, withModule = false))
            append("\n")
        }

        b {append(id) }
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

private fun documentationFor(decl: ElmTypeDeclaration): String = buildString {
    val ty = decl.typeExpressionInference().value

    definition {
        b { append("type") }
        append(" ", ty.name)
        for (param in ty.parameters) {
            append(" ", param.renderedText(linkify = true, withModule = false))
        }
        renderDefinitionLocation(decl)
    }

    renderDocContent(decl)

    val variants = decl.variantInference().value

    if (variants.isNotEmpty()) {
        sections {
            section("Variants") {
                for ((name, params) in variants) {
                    append("\n<p><code>$name</code>")
                    renderVariantParameters(params)
                }
            }
        }
    }
}

private fun documentationFor(decl: ElmTypeAliasDeclaration): String? = buildString {
    val ty = decl.typeExpressionInference().value
    val alias = ty.alias ?: return null

    definition {
        b { append("type alias") }
        append(" ").append(alias.name)
        for (type in alias.parameters) {
            append(" ", ty.renderedText(linkify = true, withModule = false))
        }
        renderDefinitionLocation(decl)
    }

    renderDocContent(decl)

    if (ty is TyRecord && ty.fields.isNotEmpty()) {
        sections {
            section("Fields") {
                for ((fieldName, fieldTy) in ty.fields) {
                    append("\n<p><code>$fieldName</code> : ")
                    append(fieldTy.renderedText(linkify = true, withModule = false))
                }
            }
        }
    }
}

private fun documentationFor(pattern: ElmLowerPattern): String? = documentationForParameter(pattern)
private fun documentationForParameter(element: ElmNamedElement): String? = buildString {
    val function = element.parentOfType<ElmFunctionDeclarationLeft>() ?: return null
    val ty = element.findTy()

    definition {
        i { append("parameter") }
        append(" ", element.name, " ")

        if (ty != null && ty !is TyUnknown) {
            append(": ", ty.renderedText(linkify = true, withModule = false), "\n")
        }

        i { append("of function ") }
        renderLink(function.name, function.name)

        renderDefinitionLocation(element)
    }
}

private fun documentationFor(element: ElmUnionVariant): String? = buildString {
    val declaration = element.parentOfType<ElmTypeDeclaration>() ?: return null
    val declTy = declaration.typeExpressionInference().value
    val name = element.name
    val variants = declaration.variantInference().value
    val params = variants[name] ?: return null

    definition {
        i { append("variant") }
        append(" ", name)
        renderVariantParameters(params)
        i { append(" of type ") }
        renderLink(declTy.name, declTy.name)
        renderDefinitionLocation(element)
    }
}

private fun documentationFor(decl: ElmModuleDeclaration): String = buildString {
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
        val declarations = ModuleScope.run { getDeclaredValues(decl.elmFile) + getDeclaredTypes(decl.elmFile) }

        // Render @docs commands
        html.replace(Regex("<p>@docs (.+?)</p>", RegexOption.DOT_MATCHES_ALL)) { match ->
            val names = match.groupValues[1].split(Regex(",\\s+"))
            names.joinToString(", ") { name ->
                declarations[name]?.let { buildString { renderLink(name, name) } } ?: name
            }
        }
    }
}

private fun documentationFor(clause: ElmAsClause): String? = buildString {
    val decl = clause.parent?.reference?.resolve() as? ElmModuleDeclaration ?: return null
    return documentationFor(decl)
}

private fun documentationFor(element: ElmInfixDeclaration): String? {
    val decl = element.funcRef?.reference?.resolve()?.parentOfType<ElmValueDeclaration>() ?: return null
    val func = decl.functionDeclarationLeft ?: return null
    return documentationFor(func)
}

private fun documentationFor(element: ElmFieldType): String? = buildString {
    val recordTy = element.parentOfType<ElmTypeAliasDeclaration>()
            ?.typeExpressionInference()?.value as? TyRecord ?: return null
    val name = element.name
    val fieldTy = recordTy.fields[name] ?: return null

    definition {
        i { append("field") }
        append(" ", name)
        if (fieldTy !is TyUnknown) {
            append(": ", fieldTy.renderedText(true, false))
        }
        if (recordTy.alias != null) {
            i { append(" of record ") }
            renderLink(recordTy.alias.name, recordTy.alias.name)
        }
        renderDefinitionLocation(element)
    }
}

private fun StringBuilder.renderVariantParameters(parameters: List<Ty>) {
    if (parameters.isNotEmpty()) {
        parameters.joinTo(this, " ", prefix = " ") {
            val renderedText = it.renderedText(linkify = true, withModule = false)
            if (it is TyUnion && it.parameters.isNotEmpty()) "($renderedText)" else renderedText
        }
    }
}

private fun StringBuilder.renderDefinitionLocation(element: ElmPsiElement) {
    val module = element.elmFile.getModuleDecl() ?: return
    i { append(" defined in ") }
    append(module.upperCaseQID.text)
}

private fun StringBuilder.renderLink(refText: String, text: String) {
    DocumentationManagerUtil.createHyperlink(this, refText, text, true)
}

private fun StringBuilder.renderDocContent(element: ElmDocTarget?, transform: (String) -> String = { it }) {
    val doc = element?.docComment ?: return

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
