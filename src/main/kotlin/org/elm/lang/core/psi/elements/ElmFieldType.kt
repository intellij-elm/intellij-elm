package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElementImpl
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.psi.IdentifierCase.LOWER


/**
 * The definition of a record field's type.
 *
 * e.g. `name : String` in the record definition `type alias Person = { name : String }`
 */
class ElmFieldType(node: ASTNode) : ElmNamedElementImpl(node, LOWER) {

    /**
     * The name of a field in a record literal type definition
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    /**
     * The definition of the type of the field.
     */
    val typeExpression: ElmTypeExpression
        get() = findNotNullChildByClass(ElmTypeExpression::class.java)

}
