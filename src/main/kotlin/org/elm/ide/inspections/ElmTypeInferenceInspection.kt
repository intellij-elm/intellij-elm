package org.elm.ide.inspections

import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.isTopLevel
import org.elm.lang.core.types.findInference

class ElmTypeInferenceInspection : ElmDiagnosticBasedInspection() {
    override fun getElementDiagnostics(element: ElmPsiElement): Iterable<ElmDiagnostic> {
        // nested declarations are taken care of in the parent inference
        if (element is ElmValueDeclaration && element.isTopLevel) {
            val inference = element.findInference() ?: return emptyList()
            return inference.diagnostics
        }

        return emptyList()
    }
}
