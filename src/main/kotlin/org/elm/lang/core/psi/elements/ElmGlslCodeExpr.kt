package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.elm.ide.injection.GlslEscaper
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes


class ElmGlslCodeExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, PsiLanguageInjectionHost {
    val content: List<PsiElement> get() = findChildrenByType(ElmTypes.GLSL_CODE_CONTENT)

    override fun updateText(text: String): PsiLanguageInjectionHost {
        val expr = ElmPsiFactory(project).createGlslExpr(text)
        node.replaceAllChildrenToChildrenOf(expr.node)
        return this
    }

    override fun createLiteralTextEscaper() = GlslEscaper(this)

    override fun isValidHost(): Boolean = true
}


