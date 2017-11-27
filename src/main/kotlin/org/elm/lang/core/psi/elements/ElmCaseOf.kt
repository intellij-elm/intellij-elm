package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl

class ElmCaseOf(node: ASTNode) : ElmPsiElementImpl(node) {

    val branches: List<ElmCaseOfBranch>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmCaseOfBranch::class.java)

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

}
