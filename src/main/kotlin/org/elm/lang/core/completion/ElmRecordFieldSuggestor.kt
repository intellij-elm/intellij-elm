package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.ElmFieldAccess
import org.elm.lang.core.types.Ty
import org.elm.lang.core.types.TyRecord
import org.elm.lang.core.types.findInference
import org.elm.lang.core.types.renderedText

object ElmRecordFieldSuggestor : Suggestor {

    override fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val file = pos.containingFile as ElmFile

        if (pos.elementType in ELM_IDENTIFIERS && parent is ElmFieldAccess) {
            // Infer the type of the record whose fields are being accessed
            // and suggest that record's fields as completion results.

            val inference = parent.findInference() ?: return

            // HACK: chained field access is not currently supported
            //       `foo.bar` => supported
            //       `foo.bar.quux` => NOT supported
            // TODO provide field completion when accessing a chain of fields
            if (parent.directChildren.count { it.elementType == ElmTypes.DOT } > 1) return

            // HACK: the type inference system does not yet keep track of the type
            //       of the sub-expression before the dot character. So we will instead
            //       just search for something in-scope with the same name.
            // TODO figure out what changes need to made to the grammar / type system to make this better
            val recordIdentifier = parent.lowerCaseIdentifierList.firstOrNull() ?: return
            val recordTy = inference.expressionTypes.filter {
                it.key.textMatches(recordIdentifier) && it.value is TyRecord
            }.values.firstOrNull()
            if (recordTy !is TyRecord) return

            // provide each field as a completion result
            recordTy.fields.forEach { fieldName, fieldTy ->
                result.add(fieldName, fieldTy)
            }
        }
    }
}

private fun CompletionResultSet.add(str: String, field: Ty) {
    addElement(LookupElementBuilder.create(str)
            .withTypeText(field.renderedText(false, false)))
}
