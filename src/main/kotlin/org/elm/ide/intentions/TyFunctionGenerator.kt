package org.elm.ide.intentions

import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*


abstract class TyFunctionGenerator(
        protected val file: ElmFile,
        protected val root: Ty
) {
    data class GeneratedFunction(
            val name: String,
            val paramTy: Ty,
            val paramName: String,
            val body: String,
            val qualifier: String
    )

    data class Ref(val module: String, val name: String)

    fun TyUnion.toRef() = Ref(module, name)
    fun AliasInfo.toRef() = Ref(module, name)
    fun DeclarationInTy.toRef() = Ref(module, name)

    /** All types and aliases referenced in the root ty */
    protected val declarations by lazy { root.allDeclarations().toList() }
    /** Additional encoder functions to generate */
    protected val funcsByTy = mutableMapOf<Ty, GeneratedFunction>()
    /** Cache of already existing callable expressions that aren't in [funcsByTy] */
    protected val callablesByTy = mutableMapOf<Ty, String>()
    /** Unions that need their variants exposed. */
    protected val unionsToExpose = mutableSetOf<Ref>()

    /** The name of the module in the current file */
    protected val moduleName by lazy { file.getModuleDecl()?.name ?: "" }

    /** The name to use for the encoder function for each type (does not include the "encoder" prefix) */
    // There might be multiple types with the same name in different modules, so add the module
    // name the function for any type with a conflict that isn't defined in this module
    protected val funcNames by lazy {
        declarations.groupBy { it.name }
                .map { (_, decls) ->
                    decls.map {
                        it.toRef() to when {
                            decls.size == 1 -> it.name
                            else -> it.module.replace(".", "") + it.name
                        }
                    }
                }.flatten().toMap()
    }

    /** Qualified names of all imported modules */
    private val importedModules: Set<String> by lazy {
        file.findChildrenByClass(ElmImportClause::class.java).mapTo(mutableSetOf()) { it.referenceName }
    }

    /** Get the module qualifier prefix to add to a name */
    protected fun qualifierFor(ref: Ref): String {
        return when (ref.module) {
            moduleName -> ""
            // We always fully qualify references to modules that we add imports for
            !in importedModules -> "${ref.module}."
            else -> ModuleScope.getQualifierForName(file, ref.module, ref.name) ?: ""
        }
    }

    /** Prefix [name], defined in [module], with the necessary qualifier */
    protected fun qual(module: String, name: String) = "${qualifierFor(Ref(module, name))}$name"

    /** Code to insert after the type annotation */
    abstract val code: String

    /** Imports to add because they are referenced by generated code. */
    val imports: List<Candidate> by lazy { calculateImports() }

    protected open fun calculateImports(): List<Candidate> {
        val visibleTypes = ModuleScope.getVisibleTypes(file).all
                .mapNotNullTo(mutableSetOf()) { it.name?.let { n -> Ref(it.moduleName, n) } }
        // Hack in List since GlobalScope.getVisibleTypes can't return it
        val visibleModules = importedModules + "List"
        return declarations
                .filter {
                    it.module != moduleName &&
                            (it.toRef() in unionsToExpose || it.module !in visibleModules && it.toRef() !in visibleTypes)
                }
                .map {
                    Candidate(
                            moduleName = it.module,
                            moduleAlias = null,
                            nameToBeExposed = if (it.isUnion) "${it.name}(..)" else ""
                    )
                }
    }

    /** Return the callable for a user-supplied function to process [ty] if there is one */
    protected fun existing(ty: Ty): String? {
        if (ty in callablesByTy) return callablesByTy[ty]!!

        ModuleScope.getRefrencableValues(file).all
                .filterIsInstance<ElmFunctionDeclarationLeft>()
                .forEach {
                    val t = it.findTy()
                    if (t != null && isExistingFunction(ty, t)) {
                        val code = qual(it.moduleName, it.name)
                        callablesByTy[ty] = code
                        return code
                    }
                }
        return null
    }

    protected abstract fun isExistingFunction(needle: Ty, function: Ty): Boolean
}

