package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmExposedValue
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.scope.ImportScope

/**
 * A value reference from an `exposing` list in an import clause (points to a different file)
 */
class ExposedValueImportReference(exposedValue: ElmExposedValue)
    : ElmReferenceCached<ElmExposedValue>(exposedValue) {

    override fun resolveInner(): ElmNamedElement? {
        return getVariants().find { it.name == element.referenceName }
    }

    override fun getVariants(): Array<ElmNamedElement> {
        val importClause = element.parentOfType<ElmImportClause>()
                ?: error("should never happen: this ref must be in an import")

        return ImportScope.fromImportDecl(importClause)
                ?.getExposedValues()?.toTypedArray()
                ?: emptyArray()
    }

}