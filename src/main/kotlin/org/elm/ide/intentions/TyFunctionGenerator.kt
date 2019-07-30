package org.elm.ide.intentions

import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*


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

abstract class TyFunctionGenerator(
        protected val file: ElmFile,
        protected val root: Ty,
        protected val functionName: String
) {
    /** All types and aliases referenced in the root ty */
    protected val declarations by lazy { root.allDeclarations().toList() }
    /** Additional encoder functions to generate */
    protected val funcsByTy = mutableMapOf<Ty, GeneratedFunction>()
    /** Unions that need their variants exposed. */
    protected val unionsToExpose = mutableSetOf<Ref>()
    /** Cache of previously generated callable expressions that aren't in [funcsByTy] */
    protected val encodersByTy = mutableMapOf<Ty, String>()

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
    protected val importedModules: Set<String> by lazy {
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

    /** Code to insert after the type annotation */
    abstract val code: String

    /** Imports to add because they are referenced by generated code. */
    abstract val imports: List<Candidate>

}

