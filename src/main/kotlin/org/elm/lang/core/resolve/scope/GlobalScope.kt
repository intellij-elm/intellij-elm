package org.elm.lang.core.resolve.scope

import com.intellij.openapi.project.Project
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.stubs.index.ElmModulesIndex
import org.elm.workspace.ElmProject


/**
 * The subset of implicitly exposed values, types and constructors provided by Elm's
 * standard library ("Core").
 */
class GlobalScope private constructor(val project: Project, val elmProject: ElmProject) {

    companion object {

        fun forElmFile(elmFile: ElmFile): GlobalScope? {
            val elmProject = elmFile.elmProject ?: return null
            if (elmFile.isCore()) {
                // The `elm/core` standard library does not have an implicit global scope. It must explicitly
                // import modules like `List`, `String`, etc.
                return null
            }
            return GlobalScope(elmFile.project, elmProject)
        }

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
                "Platform.Cmd",
                "Platform.Sub"
        )

        val defaultAliases = mapOf(
                "Cmd" to "Platform.Cmd",
                "Sub" to "Platform.Sub"
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

        fun implicitModulesMatching(name: String, elmFile: ElmFile): List<ElmModuleDeclaration> {
            val implicitModuleName = when (name) {
                in defaultImports -> name
                in defaultAliases.keys -> defaultAliases[name]
                else -> null
            } ?: return emptyList()

            return ElmModulesIndex.getAll(listOf(implicitModuleName), elmFile.project, elmFile.elmProject)
                    .filter { it.elmFile.isCore() }
        }
    }

    fun getVisibleValues(): List<ElmNamedElement> {
        // ModuleScope.getDeclaredValues is cached, so there's no need to cache the results of this
        // function.
        fun helper(moduleName: String) =
                ElmModulesIndex.get(moduleName, project, elmProject)
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
                ElmModulesIndex.get(moduleName, project, elmProject)
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
                ElmModulesIndex.get(moduleName, project, elmProject)
                        ?.let { ModuleScope(it.elmFile).getDeclaredConstructors() }
                        ?: emptyList()

        val rest = mutableListOf<ElmNamedElement>()
        rest.addAll(helper("Basics"))
        rest.addAll(helper("Maybe").filter { it.name == "Just" || it.name == "Nothing" })
        rest.addAll(helper("Result").filter { it.name == "Ok" || it.name == "Err" })
        return rest
    }
}
