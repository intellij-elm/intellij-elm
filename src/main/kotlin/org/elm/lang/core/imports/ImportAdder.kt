package org.elm.lang.core.imports

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ELM_TOP_LEVEL_DECLARATIONS
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.elements.ElmExposedType
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.resolve.scope.ModuleScope

object ImportAdder {
    /**
     * @param moduleName    the module where this value/type lives
     * @param moduleAlias   if present, the alias to use when importing [moduleName]
     * @param nameToBeExposed the name suitable for insert into an exposing clause.
     *                      Typically this is the same as `name`, but when importing
     *                      a bare union type variant, it will be the parenthesized
     *                      form: "TypeName(VariantName)"
     */
    data class Import(
            val moduleName: String,
            val moduleAlias: String?,
            val nameToBeExposed: String
    )

    /**
     * Merge an import [candidate] to the imports in [file], including exposing values if [isQualified] is true.
     */
    fun addImport(candidate: Import, file: ElmFile, isQualified: Boolean) {
        val factory = ElmPsiFactory(file.project)
        val newImport = if (isQualified)
            factory.createImport(candidate.moduleName, alias = candidate.moduleAlias)
        else
            factory.createImportExposing(candidate.moduleName, listOf(candidate.nameToBeExposed))

        val existingImport = ModuleScope.getImportDecls(file)
                .find { it.moduleQID.text == candidate.moduleName }
        if (existingImport != null) {
            // merge with existing import
            val mergedImport = mergeImports(file, existingImport, newImport)
            existingImport.replace(mergedImport)
        } else {
            // insert a new import clause
            val insertPosition = getInsertPosition(file, candidate.moduleName)
            doInsert(newImport, insertPosition)
        }
    }

    private fun doInsert(importClause: ElmImportClause, insertPosition: ASTNode) {
        val parent = insertPosition.treeParent
        val factory = ElmPsiFactory(importClause.project)
        // insert the import clause followed by a newline immediately before `insertPosition`
        val newlineNode = factory.createNewline().node
        parent.addChild(newlineNode, insertPosition)
        parent.addChild(importClause.node, newlineNode)
    }

    /**
     * Returns the node which will *follow* the new import clause
     */
    private fun getInsertPosition(file: ElmFile, moduleName: String): ASTNode {
        val existingImports = ModuleScope.getImportDecls(file)
        return when {
            existingImports.isEmpty() -> prepareInsertInNewSection(file)
            else -> getSortedInsertPosition(moduleName, existingImports)
        }
    }

    private fun prepareInsertInNewSection(sourceFile: ElmFile): ASTNode {
        // prepare for insert immediately before the first top-level declaration
        return sourceFile.node.findChildByType(ELM_TOP_LEVEL_DECLARATIONS)!!
    }

    private fun getSortedInsertPosition(moduleName: String, existingImports: List<ElmImportClause>): ASTNode {
        // NOTE: assumes that they are already sorted
        for (import in existingImports) {
            if (moduleName < import.moduleQID.text)
                return import.node
        }

        // It belongs at the end: go past the last import and its newline
        var node = existingImports.last().node.treeNext
        while (!node.textContains('\n')) {
            node = node.treeNext
        }
        return node.treeNext
    }
}


private fun mergeImports(sourceFile: ElmFile, import1: ElmImportClause, import2: ElmImportClause): ElmImportClause {
    require(import1.moduleQID.text == import2.moduleQID.text)

    val project = sourceFile.project
    val exposing1 = import1.exposingList
    val exposing2 = import2.exposingList

    val exposedNames = when {
        exposing1?.doubleDot != null || exposing2?.doubleDot != null -> listOf("..")
        // merge and sort each import's exposing clauses
        else -> sequenceOf(
                mergeExposedValues(exposing1, exposing2),
                mergeExposedOperators(exposing1, exposing2),
                mergeExposedTypes(exposing1, exposing2)
        ).flatten().toList().sorted()
    }

    // generate the new, merged import statement
    val moduleName = import1.moduleQID.text
    val modulePlusAlias = moduleName + mergeAliasClause(import1, import2)
    val factory = ElmPsiFactory(project)
    return when {
        exposedNames.isEmpty() -> factory.createImport(modulePlusAlias, null)
        else -> factory.createImportExposing(modulePlusAlias, exposedNames)
    }
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
    ).flatten().mapTo(mutableSetOf()) { it.lowerCaseIdentifier.text }.asSequence()
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
    return when {
        types.any { it.exposesAll } -> "$typeName(..)"
        else -> typeName
    }
}
