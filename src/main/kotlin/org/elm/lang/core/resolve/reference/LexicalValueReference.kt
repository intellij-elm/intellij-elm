package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ExpressionScope

/**
 * Reference to a value in lexical expression scope
 */
class LexicalValueReference(element: ElmReferenceElement)
    : ElmReferenceCached<ElmReferenceElement>(element) {

    override fun getVariants(): Array<ElmNamedElement> =
            emptyArray()

    override fun resolveInner(): ElmNamedElement? =
            getCandidates().find { it.name == element.referenceName }

    private fun getCandidates(): List<ElmNamedElement> {
        return ExpressionScope(element).getVisibleValues()
    }
}
