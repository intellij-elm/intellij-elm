package org.elm.lang.core.lookup

import org.elm.TestClientLocation
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.moduleName
import org.elm.workspace.ElmWorkspaceTestBase
import org.elm.workspace.elmWorkspace
import org.intellij.lang.annotations.Language


class ElmWorkspaceNameLookupTest : ElmWorkspaceTestBase() {

    @Language("JSON")
    private val standardElmAppProject = """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {
                        "elm/core": "1.0.0",
                        "elm/json": "1.0.0"
                    },
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                }
            }
            """.trimIndent()


    fun `test find by name`() {
        buildProject {
            project("elm.json", standardElmAppProject)
            dir("src") {
                elm("Foo.elm", """
                    module Foo exposing (..)
                    type alias Foo = ()
                """.trimIndent())
            }
        }
        check(lookup<ElmTypeAliasDeclaration>("Foo").isNotEmpty())
    }


    fun `test find by name excludes things outside of the Elm project`() {
        buildProject {
            project("elm.json", standardElmAppProject)
            dir("src") {
                elm("Main.elm")
            }
            dir("alien") {
                elm("Bar.elm", """
                    module Bar exposing (..)
                    type alias Bar = ()
                """.trimIndent())
            }
        }
        check(lookup("Bar").isEmpty())
    }


    fun `test find by name excludes things which are part of unexposed modules in a package`() {
        buildProject {
            project("elm.json", """
            {
                "type": "application",
                "source-directories": [
                    "src"
                ],
                "elm-version": "0.19.1",
                "dependencies": {
                    "direct": {
                        "elm/core": "1.0.0",
                        "elm/json": "1.0.0",
                        "elm/parser": "1.0.0"
                    },
                    "indirect": {}
                },
                "test-dependencies": {
                    "direct": {},
                    "indirect": {}
                }
            }
            """.trimIndent())
            dir("src") {
                elm("Main.elm")
            }
        }
        /*
         * elm/parser v1.0.0 includes 2 modules: "Parser" and "Parser.Advanced". However,
         * "Parser.Advanced" is not exposed by the package's `elm.json` manifest. It is important
         * that lookup only returns results belonging to exposed modules. In this case, both
         * modules define a function named "chompWhile". But lookup should only find the function
         * that is in the exposed module, "Parser".
         */
        check(lookup("chompWhile").single().moduleName == "Parser")
    }


    private fun lookup(name: String): Collection<ElmNamedElement> {
        val elmProject = project.elmWorkspace.allProjects.single()
        val clientLocation = TestClientLocation(project, elmProject)
        return ElmLookup.findByName(name, clientLocation)
    }

    @JvmName("lookupWithType")
    private inline fun <reified T : ElmNamedElement> lookup(name: String): Collection<T> =
            lookup(name).filterIsInstance<T>()
}
