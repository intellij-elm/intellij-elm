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
            val moduleName = importDecl.referenceName
            return ElmModulesIndex.get(moduleName, importDecl.elmFile)
                    ?.let { ImportScope(it.elmFile) }
        }

        /**
         * Returns an [ImportScope] for the module named by [qualifierPrefix] reachable
         * via [clientFile]. By default, the import scopes will be found by crawling the
         * explicit import declarations in [clientFile] and automatically adding the
         * implicit imports from Elm's Core standard library.
         *
         * @param qualifierPrefix The name of a module or an alias
         * @param clientFile The Elm file from which the search should be performed
         * @param importsOnly If true, include only modules reachable via imports (implicit and explicit).
         *                    Otherwise, include all modules which could be reached by the file's [ElmProject]
         */
        fun fromQualifierPrefixInModule(qualifierPrefix: String, clientFile: ElmFile, importsOnly: Boolean = true): List<ImportScope> {
            val implicitScopes = GlobalScope.implicitModulesMatching(qualifierPrefix, clientFile)
                    .map { ImportScope(it.elmFile) }

            val explicitScopes = ModuleScope.importDeclsForQualifierPrefix(clientFile, qualifierPrefix)
                    .mapNotNull { ImportScope.fromImportDecl(it) }

            return if (importsOnly) {
                implicitScopes + explicitScopes
            } else {
                val projectWideScopes = ElmModulesIndex.getAll(listOf(qualifierPrefix), clientFile)
                        .map { ImportScope(it.elmFile) }
                val allScopes = projectWideScopes + implicitScopes + explicitScopes
                allScopes.distinctBy { it.elmFile.virtualFile.path }
            }
        }
    }


    /**
     * Returns all value declarations exposed by this module.
     */
    fun getExposedValues(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredValues(elmFile)

        val exposingList = moduleDecl.exposingList
                ?: return emptyList()

        val declaredValues = ModuleScope.getDeclaredValues(elmFile)
        val exposedNames = mutableSetOf<String>()
        exposingList.exposedValueList.mapTo(exposedNames) { it.referenceName }
        exposingList.exposedOperatorList.mapTo(exposedNames) { it.referenceName }

        return declaredValues.filter { it.name in exposedNames }

    }

    /**
     * Returns all union type and type alias declarations exposed by this module.
     */
    fun getExposedTypes(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredTypes(elmFile)

        val exposedTypeList = moduleDecl.exposingList?.exposedTypeList
                ?: return emptyList()

        val exposedNames = exposedTypeList.mapTo(mutableSetOf()) { it.referenceName }
        return ModuleScope.getDeclaredTypes(elmFile).filter { it.name in exposedNames }
    }

    /**
     * Returns all union and record constructors exposed by this module.
     */
    fun getExposedConstructors(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredConstructors(elmFile)

        val exposedTypeList = moduleDecl.exposingList?.exposedTypeList
                ?: return emptyList()

        val types = ModuleScope.getDeclaredTypes(elmFile)

        return exposedTypeList.flatMap { exposedType ->
            val type = types.find { t -> t.name == exposedType.referenceName }
            val ctors = exposedType.exposedUnionConstructors
            when {
                exposedType.exposesAll -> {
                    // It's a union type that exposes all of its constructors
                    (type as? ElmTypeDeclaration)?.unionVariantList
                }

                ctors != null -> {
                    // It's a union type that exposes one or more constructors
                    (type as? ElmTypeDeclaration)?.unionVariantList
                            ?.associateBy { it.name }
                            ?.let { variants ->
                                ctors.exposedUnionConstructors.mapNotNull { variants[it.referenceName] }
                            }
                }
                else -> {
                    // It's either a record type or a union type without any exposed constructors
                    if (type is ElmTypeAliasDeclaration && type.isRecordAlias) listOf(type)
                    else null
                }
            } ?: emptyList()
        }
    }
}
