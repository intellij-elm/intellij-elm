package org.elm.lang.core.resolve.scope

import com.intellij.openapi.project.Project
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.stubs.index.ElmModules
import org.elm.workspace.ElmProject


/**
 * The subset of implicitly exposed values, types and constructors provided by Elm's
 * standard library ("Core").
 */
// TODO [kl] eventually ElmProject should be non-null, but we need to straighten out
//           some things with the integration tests and legacy Elm 0.18 projects before
//           we can be more restrictive here.
class GlobalScope(val project: Project, val elmProject: ElmProject?) {

    companion object {
        /**
         * Modules that the Elm compiler treats as being implicitly imported.
         */
        val defaultImports = listOf(
                "Basics",
                "Char",
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
        // TODO [drop 0.18] replace with `emptySet<String>()`
        val builtInValues = setOf("True", "False")

        // TODO [drop 0.18] replace with `setOf("List")`
        val builtInTypes = setOf("Bool", "String", "Char", "Int", "Float", "List")

        val allBuiltInSymbols = builtInValues.union(builtInTypes)
    }

    // TODO [kl] this is crazy inefficient, and it should be easy to cache
    // well, at least this is now stub-based, but it's still a lot of busy work and allocations.

    fun getVisibleValues(): List<ElmNamedElement> {
        fun helper(moduleName: String) =
                ElmModules.get(moduleName, project, elmProject)
                        ?.let { ModuleScope(it.elmFile).getDeclaredValues() }
                        ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("List").filter { it.name == "::" })

        // TODO [drop 0.18] remove this line (the `!` operator was removed in 0.19)
        rest.addAll(helper("Platform.Cmd").filter { it.name == "!" })

        return rest
    }


    fun getVisibleTypes(): List<ElmNamedElement> {
        fun helper(moduleName: String) =
                ElmModules.get(moduleName, project, elmProject)
                        ?.let { ModuleScope(it.elmFile).getDeclaredTypes() }
                        ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("Char").filter { it.name == "Char" })
        rest.addAll(helper("Maybe").filter { it.name == "Maybe" })
        rest.addAll(helper("Result").filter { it.name == "Result" })
        rest.addAll(helper("String").filter { it.name == "String" })
        rest.addAll(helper("Platform").filter { it.name == "Program" })
        rest.addAll(helper("Platform.Cmd").filter { it.name == "Cmd" })
        rest.addAll(helper("Platform.Sub").filter { it.name == "Sub" })
        return rest
    }


    fun getVisibleConstructors(): List<ElmNamedElement> {
        fun helper(moduleName: String) =
                ElmModules.get(moduleName, project, elmProject)
                        ?.let { ModuleScope(it.elmFile).getDeclaredConstructors() }
                        ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("Maybe").filter { it.name == "Just" || it.name == "Nothing" })
        rest.addAll(helper("Result").filter { it.name == "Ok" || it.name == "Err" })
        return rest
    }
}