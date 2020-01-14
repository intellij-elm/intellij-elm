package org.elm.lang.core.resolve.reference

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.scope.ExposedNames
import org.elm.lang.core.resolve.scope.ImportScope

/**
 * Reference from the name of a type in an import declaration's exposing list to its definition
 * in a different file.
 */
class ExposedTypeReferenceFromImport(exposedType: ElmExposedType)
    : ElmReferenceCached<ElmExposedType>(exposedType) {

    override fun resolveInner(): ElmNamedElement? {
        return getCandidates()?.get(element.referenceName)
    }

    override fun getVariants(): Array<ElmNamedElement> {
        return getCandidates()?.elements ?: emptyArray()
    }

    private fun getCandidates(): ExposedNames? {
        val importClause = element.parentOfType<ElmImportClause>()
                ?: error("should never happen: this ref must be in an import")

        return ImportScope.fromImportDecl(importClause)?.getExposedTypes()
    }
}
