package org.elm.ide.inspections

import com.intellij.psi.PsiElement
import org.elm.lang.core.diagnostics.ElmDiagnostic
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.elements.ElmTypeRef
import org.elm.lang.core.types.TypeExpression

class ElmTypeDeclarationInspection : ElmDiagnosticBasedInspection() {
    override fun getElementDiagnostics(element: PsiElement): Iterable<ElmDiagnostic> {
        return when {
            element is ElmTypeDeclaration -> TypeExpression.inferTypeDeclaration(element).diagnostics
            element is ElmTypeAliasDeclaration -> TypeExpression.inferTypeAliasDeclaration(element).diagnostics
            element is ElmTypeRef && element.parent is ElmTypeAnnotation -> TypeExpression.inferTypeRef(element).diagnostics
            else -> emptyList()
        }
    }
}
