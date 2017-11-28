package org.elm.lang.core.resolve.scope

import com.intellij.openapi.project.Project
import org.elm.lang.core.ElmModuleIndex
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmModuleDeclaration


/**
 * The subset of implicitly exposed values, types and constructors provided by Elm's
 * standard library ("Core").
 */
class GlobalScope(val project: Project) {

    companion object {
        fun implicitlyImportsModule(moduleDecl: ElmModuleDeclaration): Boolean {
            return moduleDecl.elmFile.isCore() && defaultImports.contains(moduleDecl.name)
        }

        /**
         * Modules that the Elm compiler treats as being implicitly imported.
         */
        val defaultImports = listOf(
                "Basics",
                "List",
                "Maybe",
                "Result",
                "String",
                "Tuple",
                "Debug",
                "Platform",
                "Cmd", // actually Platform.Cmd but aliased as `Cmd` by the Elm compiler
                "Sub"  // actually Platform.Sub but aliased as `Sub` by the Elm compiler
        )

        /**
         * Values and Types that are built-in to the Elm compiler. Any occurrences of
         * these symbols should be treated as always resolved.
         */
        val builtInSymbols = listOf(
                "Bool",
                "True",
                "False",
                "String",
                "Char",
                "Int",
                "Float",
                "List"
        )
    }

    // TODO [kl] this is crazy inefficient, and it should be easy to cache

    fun getVisibleValues(): List<ElmNamedElement> {
        fun helper(moduleName: String) =
            ElmModuleIndex.getFileByModuleName(moduleName, project)
                    ?.let { ModuleScope(it).getVisibleValues() }
                    ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("List").filter { it.name == "::" }) // TODO [kl] verify (::)
        rest.addAll(helper("Platform.Cmd").filter { it.name == "!" }) // TODO [kl] verify (!)
        return rest
    }


    fun getVisibleTypes(): List<ElmNamedElement> {
        fun helper(moduleName: String) =
                ElmModuleIndex.getFileByModuleName(moduleName, project)
                        ?.let { ModuleScope(it).getVisibleTypes() }
                        ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("List").filter { it.name == "List" })
        rest.addAll(helper("Maybe").filter { it.name == "Maybe" })
        rest.addAll(helper("Result").filter { it.name == "Result" })
        rest.addAll(helper("Platform").filter { it.name == "Program" })
        rest.addAll(helper("Platform.Cmd").filter { it.name == "Cmd" })
        rest.addAll(helper("Platform.Sub").filter { it.name == "Sub" })
        return rest
    }


    fun getVisibleUnionOrRecordConstructors(): List<ElmNamedElement> {
        fun helper(moduleName: String) =
                ElmModuleIndex.getFileByModuleName(moduleName, project)
                        ?.let { ModuleScope(it).getVisibleUnionOrRecordConstructors() }
                        ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("Maybe").filter { it.name == "Just" || it.name == "Nothing" })
        rest.addAll(helper("Result").filter { it.name == "Ok" || it.name == "Err" })
        return rest
    }
}