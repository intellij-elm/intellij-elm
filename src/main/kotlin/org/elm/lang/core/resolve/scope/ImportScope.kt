package org.elm.lang.core.resolve.scope

import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.stubs.index.ElmModulesIndex


/**
 * A scope that allows exposed values and types from the module named [elmFile]
 * to be imported. You can think of it as the view of the module from the outside.
 *
 * @see ModuleScope for the view of the module from inside
 */
class ImportScope(val elmFile: ElmFile) {

    companion object {

        /**
         * Returns an [ImportScope] for the module which is being imported by [importDecl].
         */
        fun fromImportDecl(importDecl: ElmImportClause): ImportScope? {
            val moduleName = importDecl.moduleQID.text
            return ElmModulesIndex.get(moduleName, importDecl.project)
                    ?.let { ImportScope(it.elmFile) }
        }

        /**
         * Returns an [ImportScope] for the module named by [qualifierPrefix] reachable
         * via [elmFile], either via import declarations or implicit imports from Elm's
         * Core standard library.
         */
        fun fromQualifierPrefixInModule(qualifierPrefix: String, elmFile: ElmFile): List<ImportScope> {
            // handle implicit imports from Core
            val implicitScopes = ElmModulesIndex.getAll(listOf(qualifierPrefix), elmFile.project)
                    .filter { it.elmFile.isCore() && qualifierPrefix in GlobalScope.defaultImports }
                    .map { ImportScope(it.elmFile) }

            // handle explicit import from within this module
            val explicitScopes = ModuleScope(elmFile).importDeclsForQualifierPrefix(qualifierPrefix)
                    .mapNotNull { ImportScope.fromImportDecl(it) }

            return implicitScopes + explicitScopes
        }
    }


    /**
     * Returns all value declarations exposed by this module.
     */
    fun getExposedValues(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope(elmFile).getDeclaredValues()

        val exposingList = moduleDecl.exposingList
                ?: return emptyList()

        val exposedValues = listOf(
                exposingList.exposedValueList,
                exposingList.exposedOperatorList)

        return exposedValues.flatten()
                .mapNotNull { it.reference?.resolve() as? ElmNamedElement }
    }

    /**
     * Returns all union type and type alias declarations exposed by this module.
     */
    fun getExposedTypes(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope(elmFile).getDeclaredTypes()

        return moduleDecl.exposingList?.exposedTypeList
                ?.mapNotNull { it.reference.resolve() as? ElmNamedElement }
                ?: emptyList()
    }

    /**
     * Returns all union and record constructors exposed by this module.
     */
    fun getExposedConstructors(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope(elmFile).getDeclaredConstructors()

        return moduleDecl.exposingList
                ?.exposedTypeList
                ?.flatMap {
                    val ctors = it.exposedUnionConstructors
                    when {
                        it.exposesAll ->
                            // It's a union type that exposes all of its constructors
                            (it.reference.resolve() as? ElmTypeDeclaration)?.unionMemberList ?: emptyList()

                        ctors != null ->
                            // It's a union type that exposes one or more constructors
                            ctors.exposedUnionConstructors.mapNotNull { it.reference.resolve() as? ElmNamedElement }

                        else -> {
                            // It's either a record type or a union type without any exposed constructors
                            val targetType = it.reference.resolve() as? ElmTypeAliasDeclaration
                            if (targetType != null && targetType.isRecordAlias)
                                listOf(targetType)
                            else
                                emptyList<ElmNamedElement>()
                        }
                    }
                }
                ?: emptyList()
    }
}
