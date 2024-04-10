package org.elm.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiReference
import org.elm.lang.core.psi.*
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.scope.ExpressionScope
import org.elm.lang.core.resolve.scope.GlobalScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.resolve.scope.QualifiedImportScope
import org.elm.lang.core.stubs.index.ElmModulesIndex


/**
 * Most completions are provided by implementing [PsiReference.getVariants],
 * but the structure of our Psi tree made it hard to provide completions for
 * qualifiable references (e.g. `Html.Events.onClick`).
 *
 * This class supplements the completions provided by the reference system.
 */
object ElmQualifiableRefSuggestor : Suggestor {

    override fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val pos = parameters.position
        val parent = pos.parent
        val grandParent = pos.parent?.parent
        val file = pos.containingFile as ElmFile

        if (grandParent is ElmValueExpr && grandParent.prevSibling is ElmNumberConstantExpr) {
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
                        ModuleScope.getVisibleConstructors(file).all.forEach { result.add(it) }
                        GlobalScope.builtInValues.forEach { result.add(it) }
                    } else {
                        /* TODO Make a distinction between completion results that are already imported
                                and those that are not. When selecting a completion that is not yet imported,
                                the import declaration should be automatically added.
                        */
                        val importScopes = QualifiedImportScope(qualifierPrefix, file, importsOnly = false)
                        importScopes.getExposedValues().forEach { result.add(it) }
                        importScopes.getExposedConstructors().forEach { result.add(it) }
                    }
                }
                is ElmUnionPattern -> {
                    if (qualifierPrefix.isEmpty()) {
                        ModuleScope.getVisibleConstructors(file).all
                                .filterIsInstance<ElmUnionVariant>()
                                .forEach { result.add(it) }
                    } else {
                        QualifiedImportScope(qualifierPrefix, file, importsOnly = false)
                                .getExposedConstructors()
                                .filterIsInstance<ElmUnionVariant>()
                                .forEach { result.add(it) }
                    }
                }
                is ElmTypeRef -> {
                    if (qualifierPrefix.isEmpty()) {
                        ModuleScope.getVisibleTypes(file).all.forEach { result.add(it) }
                        GlobalScope.builtInTypes.forEach { result.add(it) }
                    } else {
                        QualifiedImportScope(qualifierPrefix, file, importsOnly = false)
                                .getExposedTypes()
                                .forEach { result.add(it) }
                    }
                }
            }
        }
    }

    private fun suggestQualifiers(qualifierPrefix: String, file: ElmFile, result: CompletionResultSet) {
        // Get all modules exposed to this Elm project, regardless of whether they have been imported,
        // and suggest them hierarchically based on the dotted module name.
        //
        // EXAMPLE:
        // assume that the Elm project has a dependency on elm/json which provides Json.Decode and Json.Encode modules
        // if the input text is "Jso" then we would suggest "Json"
        // and if the input text is "Json." then we might suggest "Decode" and "Encode"
        ElmModulesIndex.getAll(file).asSequence()
                .filter { it.name.startsWith(qualifierPrefix) && it.name != qualifierPrefix }
                .map { it.name.removePrefix("$qualifierPrefix.").substringBefore('.') }
                .forEach { result.add(it) }

        // Aliases are forbidden from having dots in the name. So if the qualifier prefix is empty, then
        // we are in a state where aliases can (and should) be suggested.
        if (qualifierPrefix.isEmpty()) {
            ModuleScope.getAliasDecls(file).forEach { result.add(it) }
        }
    }

}

private fun CompletionResultSet.add(str: String) {
    addElement(LookupElementBuilder.create(str))
}

private fun CompletionResultSet.add(element: ElmNamedElement) {
    addElement(LookupElementBuilder.create(element))
}

