
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.QualifiedConstructorReference
import org.elm.lang.core.resolve.reference.QualifiedModuleNameReference
import org.elm.lang.core.resolve.reference.SimpleUnionConstructorReference


class ElmUnionPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    /** The union constructor */
    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    /** pattern matching on the arguments (if any) to the union constructor */
    val patternList: List<ElmPattern>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmPattern::class.java)


    override val referenceNameElement: PsiElement
        get() {
            return upperCaseQID.upperCaseIdentifierList.last()
        }

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            references.first()

    override fun getReferences() =
            if (upperCaseQID.isQualified)
                arrayOf(QualifiedConstructorReference(this, upperCaseQID),
                        QualifiedModuleNameReference(this, upperCaseQID))
            else
                arrayOf(SimpleUnionConstructorReference(this))

}
