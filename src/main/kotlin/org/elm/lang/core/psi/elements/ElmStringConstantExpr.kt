package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.elm.ide.injection.ElmStringEscaper
import org.elm.lang.core.psi.*


/** A literal string. e.g. `""` or `"""a"b"""` */
class ElmStringConstantExpr(node: ASTNode) : ElmPsiElementImpl(node), PsiLanguageInjectionHost, ElmConstantTag {
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

    override fun isValidHost(): Boolean = true

    override fun updateText(text: String): PsiLanguageInjectionHost {
        val expr = ElmPsiFactory(project).createStringConstant(text)
        node.replaceAllChildrenToChildrenOf(expr.node)
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<ElmStringConstantExpr> {
        return ElmStringEscaper(this, !text.startsWith("\"\"\""))
    }
}
