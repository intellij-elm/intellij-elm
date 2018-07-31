package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.markup.GutterDraggableObject.flavor
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.elements.Flavor.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.*

/**
 * The name of an existing record which is to being updated.
 *
 * e.g. the first instance of person in `{ person | age = person.age + 1 }`
 */
class ElmRecordBaseIdentifier(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {
    override val referenceNameElement: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): ElmReference = references.first()
    override fun getReferences(): Array<ElmReference> = arrayOf(LexicalValueReference(this))
}
