package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.ModuleNameQualifierReference
import org.elm.lang.core.resolve.reference.QualifiedConstructorReference
import org.elm.lang.core.resolve.reference.SimpleUnionConstructorReference


/**
 * A pattern that matches on the value of a union type
 *
 * e.g. `Just a` or `Nothing` when used as a function parameter or case pattern
 */
class ElmUnionPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement, ElmPatternChildTag {

    /** The union constructor */
    val upperCaseQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    /** pattern matching on the arguments (if any) to the union constructor */
    val argumentPatterns: Sequence<ElmUnionPatternChildTag>
        get() = directChildren.filterIsInstance<ElmUnionPatternChildTag>()

    /** All named elements introduced by this pattern */
    val namedParameters: List<ElmNameDeclarationPatternTag>
        get() = PsiTreeUtil.collectElementsOfType(this, ElmNameDeclarationPatternTag::class.java).toList()

    override val referenceNameElement: PsiElement
        get() = upperCaseQID.upperCaseIdentifierList.last()

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference =
            references.first()

    override fun getReferences(): Array<ElmReference> =
            if (upperCaseQID.isQualified)
                arrayOf(QualifiedConstructorReference(this, upperCaseQID),
                        ModuleNameQualifierReference(this, upperCaseQID, upperCaseQID.qualifierPrefix))
            else
                arrayOf(SimpleUnionConstructorReference(this))

}
