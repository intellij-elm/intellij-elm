package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.elm.ide.injection.ElmStringEscaper
import org.elm.lang.core.psi.*
import org.intellij.lang.regexp.DefaultRegExpPropertiesProvider
import org.intellij.lang.regexp.RegExpLanguageHost
import org.intellij.lang.regexp.psi.RegExpChar
import org.intellij.lang.regexp.psi.RegExpGroup
import org.intellij.lang.regexp.psi.RegExpNamedGroupRef


/** A literal string. e.g. `""` or `"""a"b"""` */
class ElmStringConstantExpr(node: ASTNode) : ElmPsiElementImpl(node), PsiLanguageInjectionHost, RegExpLanguageHost, ElmConstantTag {
    /** The elements that make up the string, excluding the opening or closing quotes */
    val content: Sequence<PsiElement>
        get() =
            directChildren.filter { child ->
                child.elementType.let {
                    it == ElmTypes.REGULAR_STRING_PART ||
                            it == ElmTypes.STRING_ESCAPE ||
                            it == ElmTypes.INVALID_STRING_ESCAPE
                }
            }

    /** The offset, relative to this element, of the content of the string (everything except the quotation marks)*/
    val contentOffsets: TextRange get() = when {
        text.startsWith("\"\"\"") -> TextRange(3, text.length - 3)
        else -> TextRange(1, text.length - 1)
    }

    override fun isValidHost(): Boolean = true

    override fun updateText(text: String): PsiLanguageInjectionHost {
        val expr = ElmPsiFactory(project).createStringConstant(text)
        node.replaceAllChildrenToChildrenOf(expr.node)
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<ElmStringConstantExpr> {
        return ElmStringEscaper(this, !text.startsWith("\"\"\""))
    }

    override fun characterNeedsEscaping(c: Char): Boolean = false
    override fun supportsPerl5EmbeddedComments(): Boolean = false
    override fun supportsPossessiveQuantifiers(): Boolean = true
    override fun supportsPythonConditionalRefs(): Boolean = false
    override fun supportsNamedGroupSyntax(group: RegExpGroup): Boolean = true

    override fun supportsNamedGroupRefSyntax(ref: RegExpNamedGroupRef): Boolean =
            ref.isNamedGroupRef

    override fun supportsExtendedHexCharacter(regExpChar: RegExpChar): Boolean = true

    override fun isValidCategory(category: String): Boolean =
            DefaultRegExpPropertiesProvider.getInstance().isValidCategory(category)

    override fun getAllKnownProperties(): Array<Array<String>> =
            DefaultRegExpPropertiesProvider.getInstance().allKnownProperties

    override fun getPropertyDescription(name: String?): String? =
            DefaultRegExpPropertiesProvider.getInstance().getPropertyDescription(name)

    override fun getKnownCharacterClasses(): Array<Array<String>> =
            DefaultRegExpPropertiesProvider.getInstance().knownCharacterClasses
}
