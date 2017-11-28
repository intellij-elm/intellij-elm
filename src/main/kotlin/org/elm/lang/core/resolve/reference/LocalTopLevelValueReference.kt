package org.elm.lang.core.resolve.reference

import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.scope.ModuleScope

/**
 * A reference from the left-hand-side of a type annotation to a LOCALLY declared
 * value or function name.
 */
class LocalTopLevelValueReference(element: ElmReferenceElement): ElmReferenceBase<ElmReferenceElement>(element) {

    override fun getVariants() =
            ModuleScope(element.elmFile).getDeclaredValues().toTypedArray()
}