package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.elm.lang.core.psi.ELM_IDENTIFIERS
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.ElmTypes.NUMBER_LITERAL
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmParametricTypeRef
import org.elm.lang.core.psi.elements.ElmUnionMember
import org.elm.lang.core.psi.elements.ElmUnionPattern
import org.elm.lang.core.psi.elements.ElmUpperPathTypeRef
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.resolve.scope.ExpressionScope
import org.elm.lang.core.resolve.scope.GlobalScope
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.stubs.index.ElmModulesIndex


/**
 * Most completions are provided by implementing [PsiReference.getVariants],
 * but there are some things that cannot be expressed that way (keywords)
 * or are difficult to express (qualified names).
 *
 * This class supplements the completions provided by the reference system.
 */
class ElmCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val grandParent = pos.parent?.parent
        val file = pos.containingFile as ElmFile

        if (grandParent is ElmValueExpr && grandParent.prevSibling?.elementType == NUMBER_LITERAL) {
            /*
            Ignore this case in order to prevent IntelliJ from suggesting completions
            when the caret is immediately after a number.
            */
        } else if (pos.elementType in ELM_IDENTIFIERS && parent is ElmQID) {
            val qualifierPrefix = parent.qualifierPrefix
            suggestQualifiers(qualifierPrefix, file, result)

            when (grandParent) {
                is ElmValueExpr -> {
                    if (qualifierPrefix.isEmpty()) {
                        ExpressionScope(parent).getVisibleValues().forEach { result.add(it) }
                        ModuleScope(file).getVisibleConstructors().forEach { result.add(it) }
                        GlobalScope.builtInValues.forEach { result.add(it) }
                    } else {
                        val importScope = ImportScope.fromQualifierPrefixInModule(qualifierPrefix, file)
                        importScope?.getExposedValues()?.forEach { result.add(it) }
                        importScope?.getExposedConstructors()?.forEach { result.add(it) }
                    }
                }
                is ElmUnionPattern -> {
                    if (qualifierPrefix.isEmpty()) {
                        ModuleScope(file).getVisibleConstructors()
                                .filter { it is ElmUnionMember }
                                .forEach { result.add(it) }
                    } else {
                        val importScope = ImportScope.fromQualifierPrefixInModule(qualifierPrefix, file)
                        importScope?.getExposedConstructors()
                                ?.filter { it is ElmUnionMember }
                                ?.forEach { result.add(it) }
                    }
                }
                is ElmUpperPathTypeRef, is ElmParametricTypeRef -> {
                    if (qualifierPrefix.isEmpty()) {
                        ModuleScope(file).getVisibleTypes().forEach { result.add(it) }
                        GlobalScope.builtInTypes.forEach { result.add(it) }
                    } else {
                        val importScope = ImportScope.fromQualifierPrefixInModule(qualifierPrefix, file)
                        importScope?.getExposedTypes()?.forEach { result.add(it) }
                    }
                }
            }
        }
    }

    private fun suggestQualifiers(qualifierPrefix: String, file: ElmFile, result: CompletionResultSet) {
        ElmModulesIndex.getAll(file.project)
                .filter { it.name.startsWith(qualifierPrefix) && it.name != qualifierPrefix }
                .map { it.name.removePrefix("$qualifierPrefix.").substringBefore('.') }
                .forEach { result.add(it) }
    }

}

private fun CompletionResultSet.add(str: String) {
    addElement(LookupElementBuilder.create(str))
}

private fun CompletionResultSet.add(element: ElmNamedElement) {
    addElement(LookupElementBuilder.create(element))
}

