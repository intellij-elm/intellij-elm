package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.elm.lang.core.diagnostics.TypeMismatchError
import org.elm.lang.core.psi.ELM_IDENTIFIERS
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmField
import org.elm.lang.core.psi.elements.ElmRecordExpr
import org.elm.lang.core.types.*

object ElmRecordLiteralFieldSuggestor : Suggestor {

    override fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val grandParent = parent?.parent
        val recordExpr = parent as? ElmRecordExpr
                ?: if (parent is ElmField) grandParent as? ElmRecordExpr else null

        if (pos.elementType in ELM_IDENTIFIERS && recordExpr != null) {
            recordExpr.findInference()
                    ?.diagnostics
                    ?.filterIsInstance<TypeMismatchError>()
                    ?.filter { it.recordDiff != null }
                    ?.firstOrNull { it.element === recordExpr }
                    ?.recordDiff
                    ?.missing
                    ?.forEach { fieldName, fieldTy ->
                        result.add(fieldName, fieldTy, parent is ElmField)
                    }
        }
    }
}

private fun CompletionResultSet.add(fieldName: String, field: Ty, inElmField: Boolean) {
    // Do not add equals after key in a field { {-caret-} = "hello" }
    val str = if (inElmField) fieldName else "$fieldName = "
    addElement(LookupElementBuilder.create(str)
            .withTypeText(field.renderedText()))
}
