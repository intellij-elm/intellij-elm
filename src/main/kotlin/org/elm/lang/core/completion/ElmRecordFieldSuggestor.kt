package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.ElmFieldAccessExpr
import org.elm.lang.core.types.*

object ElmRecordFieldSuggestor : Suggestor {

    override fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val file = pos.containingFile as ElmFile

        if (pos.elementType in ELM_IDENTIFIERS && parent is ElmFieldAccessExpr) {
            // Infer the type of the record whose fields are being accessed
            // and suggest that record's fields as completion results.

            val ty = parent.targetExpr.findTy() as? TyRecord ?: return

            // provide each field as a completion result
            ty.fields.forEach { fieldName, fieldTy ->
                result.add(fieldName, fieldTy)
            }
        }
    }
}

private fun CompletionResultSet.add(str: String, field: Ty) {
    addElement(LookupElementBuilder.create(str)
            .withTypeText(field.renderedText(false, false)))
}
