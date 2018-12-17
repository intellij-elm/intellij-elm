package org.elm.ide.inspections

import com.intellij.psi.PsiElement
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.types.typeExpressionInference

class ElmTypeDeclarationInspection : ElmDiagnosticBasedInspection() {
    override fun getElementDiagnostics(element: PsiElement): Iterable<ElmDiagnostic> {
        return when (element) {
            is ElmTypeDeclaration -> element.typeExpressionInference().diagnostics
            is ElmTypeAliasDeclaration -> element.typeExpressionInference().diagnostics
            is ElmTypeAnnotation -> element.typeExpressionInference()?.diagnostics ?: emptyList()
            else -> emptyList()
        }
    }
}
