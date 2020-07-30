package org.elm.lang.core.resolve.scope

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.ParameterizedCachedValue
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.psi.globalModificationTracker
import org.elm.lang.core.stubs.index.ElmModulesIndex

private val EXPOSED_VALUES_KEY: Key<ParameterizedCachedValue<ExposedNames, ElmFile>> = Key.create("EXPOSED_VALUES_KEY")
private val EXPOSED_TYPES_KEY: Key<ParameterizedCachedValue<ExposedNames, ElmFile>> = Key.create("EXPOSED_TYPES_KEY")
private val EXPOSED_CONSTRUCTORS_KEY: Key<ParameterizedCachedValue<ExposedNames, ElmFile>> = Key.create("EXPOSED_CONSTRUCTORS_KEY")

class ExposedNames(val elements: Array<ElmNamedElement>) {
    constructor(elements: List<ElmNamedElement>) : this(elements.toTypedArray())

    private val byName = elements.associateByTo(mutableMapOf()) { it.name }.apply { remove(null) }
    operator fun get(key: String?) = byName[key]
}

/**
 * A scope of exposed names for the module named by [qualifierPrefix] reachable
 * via [clientFile]. By default, the import scopes will be found by crawling the
 * explicit import declarations in [clientFile] and automatically adding the
 * implicit imports from Elm's Core standard library.
 *
 * @param qualifierPrefix The name of a module or an alias
 * @param clientFile The Elm file from which the search should be performed
 * @param importsOnly If true, include only modules reachable via imports (implicit and explicit).
 *                    Otherwise, include all modules which could be reached by the file's [ElmProject]
 */
class QualifiedImportScope(
        private val qualifierPrefix: String,
        private val clientFile: ElmFile,
        private val importsOnly: Boolean = true
) {
    fun getExposedValue(name: String): ElmNamedElement? {
        return scopes().mapNotNull { it.getExposedValues()[name] }.firstOrNull()
    }

    fun getExposedValues(): Sequence<ElmNamedElement> {
        return scopes().flatMap { it.getExposedValues().elements.asSequence() }
    }

    fun getExposedType(name: String): ElmNamedElement? {
        return scopes().mapNotNull { it.getExposedTypes()[name] }.firstOrNull()
    }

    fun getExposedTypes(): Sequence<ElmNamedElement> {
        return scopes().flatMap { it.getExposedTypes().elements.asSequence() }
    }

    fun getExposedConstructor(name: String): ElmNamedElement? {
        return scopes().mapNotNull { it.getExposedConstructors()[name] }.firstOrNull()
    }

    fun getExposedConstructors(): Sequence<ElmNamedElement> {
        return scopes().flatMap { it.getExposedConstructors().elements.asSequence() }
    }

    /** Lazily yield all individual scopes that can contain exposed names with this prefix */
    private fun scopes(): Sequence<ImportScope> = sequence {
        if (importsOnly) {
            yieldAll(explicitScopes())
            yieldAll(implicitScopes())
        } else {
            val projectWideScopes = ElmModulesIndex.getAll(listOf(qualifierPrefix), clientFile)
                    .asSequence().map { ImportScope(it.elmFile) }
            val allScopes = explicitScopes() + implicitScopes() + projectWideScopes
            yieldAll(allScopes.distinctBy { it.elmFile.virtualFile.path })
        }
    }

    private fun implicitScopes() = GlobalScope.implicitModulesMatching(qualifierPrefix, clientFile)
            .asSequence().map { ImportScope(it.elmFile) }

    private fun explicitScopes() = ModuleScope.importDeclsForQualifierPrefix(clientFile, qualifierPrefix)
            .asSequence().mapNotNull { ImportScope.fromImportDecl(it) }
}

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
    }

    /**
     * Returns all value declarations exposed by this module.
     */
    fun getExposedValues(): ExposedNames {
        return CachedValuesManager.getManager(elmFile.project).getParameterizedCachedValue(elmFile, EXPOSED_VALUES_KEY, {
            CachedValueProvider.Result.create(ExposedNames(produceExposedValues()), it.globalModificationTracker)
        }, /*trackValue*/ false, /*parameter*/ elmFile)
    }

    private fun produceExposedValues(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredValues(elmFile).list

        val exposingList = moduleDecl.exposingList
                ?: return emptyList()

        val declaredValues = ModuleScope.getDeclaredValues(elmFile).array
        val exposedNames = mutableSetOf<String>()
        exposingList.exposedValueList.mapTo(exposedNames) { it.referenceName }
        exposingList.exposedOperatorList.mapTo(exposedNames) { it.referenceName }

        return declaredValues.filter { it.name in exposedNames }
    }

    /**
     * Returns all union type and type alias declarations exposed by this module.
     */
    fun getExposedTypes(): ExposedNames {
        return CachedValuesManager.getManager(elmFile.project).getParameterizedCachedValue(elmFile, EXPOSED_TYPES_KEY, {
            CachedValueProvider.Result.create(ExposedNames(produceExposedTypes()), it.globalModificationTracker)
        }, false, elmFile)
    }

    private fun produceExposedTypes(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredTypes(elmFile).list

        val exposedTypeList = moduleDecl.exposingList?.exposedTypeList
                ?: return emptyList()

        val exposedNames = exposedTypeList.mapTo(mutableSetOf()) { it.referenceName }
        return ModuleScope.getDeclaredTypes(elmFile).array.filter { it.name in exposedNames }
    }

    /**
     * Returns all union and record constructors exposed by this module.
     */
    fun getExposedConstructors(): ExposedNames {
        return CachedValuesManager.getManager(elmFile.project).getParameterizedCachedValue(elmFile, EXPOSED_CONSTRUCTORS_KEY, {
            CachedValueProvider.Result.create(ExposedNames(produceExposedConstructors()), it.globalModificationTracker)
        }, false, elmFile)
    }

    private fun produceExposedConstructors(): List<ElmNamedElement> {
        val moduleDecl = elmFile.getModuleDecl()
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope.getDeclaredConstructors(elmFile).list

        val exposedTypeList = moduleDecl.exposingList?.exposedTypeList
                ?: return emptyList()

        val types = ModuleScope.getDeclaredTypes(elmFile)

        return exposedTypeList.flatMap { exposedType ->
            val type = types[exposedType.referenceName]
            when {
                exposedType.exposesAll -> {
                    // It's a union type that exposes all of its constructors
                    (type as? ElmTypeDeclaration)?.unionVariantList
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
