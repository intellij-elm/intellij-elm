package org.elm.ide.intentions

import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmImportClause


fun mergeImports(sourceFile: ElmFile, import1: ElmImportClause, import2: ElmImportClause): ElmImportClause {
    require(import1.moduleQID.text == import2.moduleQID.text)

    val project = sourceFile.getProject()
    val exposing1 = import1.exposingList
    val exposing2 = import2.exposingList

    // merge and sort each import's exposing clauses
    val exposedNames = sequenceOf(
            mergeExposedValues(exposing1, exposing2),
            mergeExposedOperators(exposing1, exposing2),
            mergeExposedTypes(exposing1, exposing2)
    ).flatten().toList().sorted()

    // generate the new, merged import statement
    val moduleName = import1.moduleQID.text
    val modulePlusAlias = moduleName + mergeAliasClause(import1, import2)
    return ElmPsiFactory(project).createImportExposing(modulePlusAlias, exposedNames)
}


private fun mergeAliasClause(import1: ElmImportClause, import2: ElmImportClause): String {
    return (import1.asClause ?: import2.asClause)
            ?.let { " as " + it.name }
            ?: ""
}


private fun mergeExposedValues(exposing1: ElmExposingList?, exposing2: ElmExposingList?): Sequence<String> {
    return sequenceOf(
            exposing1?.exposedValueList ?: emptyList(),
            exposing2?.exposedValueList ?: emptyList()
    ).flatten().map { it.lowerCaseIdentifier.text }
}


private fun mergeExposedOperators(exposing1: ElmExposingList?, exposing2: ElmExposingList?): Sequence<String> {
    return sequenceOf(
            exposing1?.exposedOperatorList ?: emptyList(),
            exposing2?.exposedOperatorList ?: emptyList()
    ).flatten().map { "(${it.operatorIdentifier.text})" }
}


private fun mergeExposedTypes(exposing1: ElmExposingList?, exposing2: ElmExposingList?): Sequence<String> {
    val exposedUnionsByName: Map<String, List<ElmExposedType>> =
            sequenceOf(
                    exposing1?.exposedTypeList ?: emptyList(),
                    exposing2?.exposedTypeList ?: emptyList()
            ).flatten().groupBy { it.referenceName }

    return exposedUnionsByName
            .map { (typeName, types) -> mergeExposedUnionConstructors(typeName, types) }
            .asSequence()
}


private fun mergeExposedUnionConstructors(typeName: String, types: List<ElmExposedType>): String {
    if (types.any { it.exposesAll })
        return typeName + "(..)"

    val body = types
            .map { it.exposedUnionConstructors }
            .filterNotNull()
            .flatMap { it.exposedUnionConstructors }
            .map { it.text }
            .sorted()
            .joinToString(", ")

    return if (body.isEmpty()) typeName else "$typeName($body)"
}