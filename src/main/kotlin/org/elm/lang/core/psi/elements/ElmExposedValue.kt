package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.lang.core.resolve.reference.ExposedValueImportReference
import org.elm.lang.core.resolve.reference.ExposedValueModuleReference


/**
 * A value or function exposed via an import clause's `exposing` list.
 *
 * e.g. `bar` in `import Foo exposing (bar)`
 */
class ElmExposedValue(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)


    override val referenceNameElement: PsiElement
        get() = lowerCaseIdentifier

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference {
        // TODO [kl] cleanup
        val moduleDecl = parentOfType<ElmModuleDeclaration>()
        return if (moduleDecl != null)
            ExposedValueModuleReference(this)
        else
            ExposedValueImportReference(this)
    }
}
