package org.elm.lang.core.resolve.scope

import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmTypeDeclaration


/**
 * A scope that provides access to declarations within the module as well as declarations
 * that have been imported into the module. You can think of it as the view of the module
 * from the inside
 *
 * @see ImportScope for the view of the module from the outside
 */
class ModuleScope(val elmFile: ElmFile) {

    fun getImportDecls() =
            elmFile.descendantsOfType<ElmImportClause>()

    fun getAliasDecls() =
            getImportDecls().mapNotNull { it.asClause }


    /**
     * Finds the import declaration (if any) named by [qualifierPrefix].
     *
     * The [qualifierPrefix] may be either a module name or an import alias.
     */
    fun importDeclForQualifierPrefix(qualifierPrefix: String) =
            getImportDecls().find {
                it.moduleQID.text == qualifierPrefix || it.asClause?.name == qualifierPrefix
            }


    // VALUES


    fun getDeclaredValues(): List<ElmNamedElement> {
        val valueDecls = elmFile.getValueDeclarations().flatMap {
            it.declaredNames(includeParameters = false)
        }
        return listOf(valueDecls, elmFile.getPortAnnotations(), elmFile.getInfixDeclarations())
                .flatten()
    }


    fun getVisibleValues(): List<ElmNamedElement> {
        val globallyExposedValues =
        // TODO [kl] re-think this lame hack to avoid an infinite loop
                if (elmFile.isCore())
                    emptyList()
                else
                    GlobalScope(elmFile.project).getVisibleValues()
        val topLevelValues = getDeclaredValues()
        val importedValues = elmFile.findChildrenByClass(ElmImportClause::class.java)
                .flatMap { getVisibleImportNames(it) }
        return listOf(globallyExposedValues, topLevelValues, importedValues).flatten()
    }


    private fun getVisibleImportNames(importClause: ElmImportClause): List<ElmNamedElement> {
        val allExposedValues = ImportScope.fromImportDecl(importClause)
                ?.getExposedValues()
                ?: return emptyList()

        if (importClause.exposesAll)
            return allExposedValues

        // intersect the names exposed by the module with the names declared
        // in this import clause's exposing list.
        val locallyExposedNames = importClause.exposingList?.exposedValueList
                ?.map { it.lowerCaseIdentifier.text }
                ?.toSet()
                ?: emptySet()

        val locallyExposedOperators = importClause.exposingList?.exposedOperatorList
                ?.map { it.operatorIdentifier.text }
                ?.toSet()
                ?: emptySet()

        return allExposedValues.filter {
            it.name in locallyExposedNames || it.name in locallyExposedOperators
        }
    }


    // TYPES


    fun getDeclaredTypes(): List<ElmNamedElement> {
        return elmFile.getTypeDeclarations() + elmFile.getTypeAliasDeclarations()
    }


    fun getVisibleTypes(): List<ElmNamedElement> {
        val globallyExposedTypes =
        // TODO [kl] re-think this lame hack to avoid an infinite loop
                if (elmFile.isCore())
                    emptyList()
                else
                    GlobalScope(elmFile.project).getVisibleTypes()
        val topLevelTypes = getDeclaredTypes()
        val importedTypes = elmFile.findChildrenByClass(ElmImportClause::class.java)
                .flatMap { getVisibleImportTypes(it) }
        return listOf(globallyExposedTypes, topLevelTypes, importedTypes).flatten()
    }


    private fun getVisibleImportTypes(importClause: ElmImportClause): List<ElmNamedElement> {
        val allExposedTypes = ImportScope.fromImportDecl(importClause)
                ?.getExposedTypes()
                ?: return emptyList()

        if (importClause.exposesAll)
            return allExposedTypes

        // intersect the names exposed by the module with the names declared
        // in this import clause's exposing list.
        val locallyExposedNames = importClause.exposingList?.exposedTypeList
                ?.map { it.upperCaseIdentifier.text }
                ?.toSet() ?: emptySet()
        return allExposedTypes.filter { locallyExposedNames.contains(it.name) }
    }


    // UNION CONSTRUCTORS AND RECORD CONSTRUCTORS


    fun getDeclaredConstructors(): List<ElmNamedElement> {
        return listOf(
                elmFile.getTypeDeclarations().flatMap { it.unionMemberList },
                elmFile.getTypeAliasDeclarations().filter { it.isRecordAlias }
        ).flatten()
    }


    fun getVisibleConstructors(): List<ElmNamedElement> {
        val globallyExposedConstructors =
        // TODO [kl] re-think this lame hack to avoid an infinite loop
                if (elmFile.isCore())
                    emptyList()
                else
                    GlobalScope(elmFile.project).getVisibleConstructors()
        val topLevelConstructors = getDeclaredConstructors()
        val importedConstructors = elmFile.findChildrenByClass(ElmImportClause::class.java)
                .flatMap { getVisibleImportConstructors(it) }
        return listOf(globallyExposedConstructors, topLevelConstructors, importedConstructors).flatten()
    }

    private fun getVisibleImportConstructors(importClause: ElmImportClause): List<ElmNamedElement> {
        val allExposedConstructors = ImportScope.fromImportDecl(importClause)
                ?.getExposedConstructors()
                ?: return emptyList()

        if (importClause.exposesAll)
            return allExposedConstructors

        val exposedTypes = importClause.exposingList?.exposedTypeList
                ?: return emptyList()

        // Intersect the names exposed by the module with the names declared
        // in this import clause's exposing list, making special consideration for
        // union types that expose all of their constructors.

        val locallyExposedUnionConstructorNames =
                exposedTypes.flatMap {
                    when {
                        it.exposesAll ->
                            (it.reference.resolve() as? ElmTypeDeclaration)
                                    ?.unionMemberList
                                    ?.map { it.name }
                                    ?: emptyList()

                        else ->
                            it.exposedUnionConstructors?.exposedUnionConstructors
                                    ?.map { it.upperCaseIdentifier.text }
                                    ?: emptyList()
                    }
                }

        val locallyExposedRecordConstructorNames = exposedTypes
                .mapNotNull {
                    if (it.exposedUnionConstructors == null)
                        it.upperCaseIdentifier.text
                    else
                        null
                }.toSet()

        val allExposedNames = locallyExposedUnionConstructorNames.union(locallyExposedRecordConstructorNames)
        return allExposedConstructors.filter { allExposedNames.contains(it.name) }
    }
}
