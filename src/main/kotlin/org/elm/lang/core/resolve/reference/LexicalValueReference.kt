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

    override fun resolveInner(): ElmNamedElement? {
        val resolved = resolveShallow()
        return (resolved as? ElmReferenceElement)?.reference?.resolve() ?: resolved
    }

    /**
     * References of this type normally recursively resolve references to elements that are
     * themselves references (e.g. usage of a name in a record destructuring pattern). This function
     * will only resolve the first level of reference.
     */
    fun resolveShallow(): ElmNamedElement? {
        val referenceName = element.referenceName
        return ExpressionScope(element).getVisibleValues().find { it.name == referenceName }
    }
}
