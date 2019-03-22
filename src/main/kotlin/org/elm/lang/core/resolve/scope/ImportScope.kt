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
            return ModuleScope.getDeclaredTypes(elmFile)

        return moduleDecl.exposingList?.exposedTypeList
                ?.mapNotNull { it.reference.resolve() }
                ?: emptyList()
    }

    /**
     * Returns all union and record constructors exposed by this module.
     */
    fun getExposedConstructors(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredConstructors(elmFile)

        return moduleDecl.exposingList
                ?.exposedTypeList
                ?.flatMap {
                    val ctors = it.exposedUnionConstructors
                    when {
                        it.exposesAll ->
                            // It's a union type that exposes all of its constructors
                            (it.reference.resolve() as? ElmTypeDeclaration)?.unionVariantList ?: emptyList()

                        ctors != null ->
                            // It's a union type that exposes one or more constructors
                            ctors.exposedUnionConstructors.mapNotNull { it.reference.resolve() }

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
