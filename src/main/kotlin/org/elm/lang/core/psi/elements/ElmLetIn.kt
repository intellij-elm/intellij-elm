
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl


class ElmLetIn(node: ASTNode) : ElmPsiElementImpl(node) {

    val expression: ElmExpression
        get() = findNotNullChildByClass(ElmExpression::class.java)

    val innerTypeAnnotationList: List<ElmInnerTypeAnnotation>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerTypeAnnotation::class.java)

    val innerValueDeclarationList: List<ElmInnerValueDeclaration>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmInnerValueDeclaration::class.java)

}
