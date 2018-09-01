package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmConsPatternChildTag
import org.elm.lang.core.psi.ElmPatternChildTag
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.QualifiedConstructorReference
import org.elm.lang.core.resolve.reference.QualifiedModuleNameReference
import org.elm.lang.core.resolve.reference.SimpleUnionConstructorReference


/**
 * A pattern that matches on the value of a union type
 *
 * e.g. `Maybe a` or `Just` when used as a function parameter or case pattern
 */
class ElmUnionPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmConsPatternChildTag, ElmPatternChildTag {

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

    override fun getReference(): ElmReference =
            references.first()

    override fun getReferences(): Array<ElmReference> =
            if (upperCaseQID.isQualified)
                arrayOf(QualifiedConstructorReference(this, upperCaseQID),
                        QualifiedModuleNameReference(this, upperCaseQID))
            else
                arrayOf(SimpleUnionConstructorReference(this))

}
