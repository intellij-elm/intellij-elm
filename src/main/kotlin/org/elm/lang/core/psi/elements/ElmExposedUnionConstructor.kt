package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ExposedUnionConstructorReference


/**
 * A union constructor explicitly exposed from an exposing list
 *
 * e.g. `Home` in `import App exposing Page(Home)`
 */
class ElmExposedUnionConstructor(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val upperCaseIdentifier: PsiElement
        get() = findNotNullChildByType(UPPER_CASE_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = upperCaseIdentifier

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            ExposedUnionConstructorReference(this)
}
