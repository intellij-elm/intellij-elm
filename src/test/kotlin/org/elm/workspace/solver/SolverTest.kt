package org.elm.workspace.solver

import org.elm.workspace.Constraint
import org.elm.workspace.Version
import org.junit.Assert.assertEquals
import org.junit.Test

private val fixture = Repository(
        Pkg(name = "B", version = v(1, 0, 0), dependencies = emptyMap()),
        Pkg(name = "B", version = v(1, 0, 1), dependencies = emptyMap()),
        Pkg(name = "B", version = v(1, 0, 2), dependencies = emptyMap()),
        Pkg(name = "B", version = v(1, 0, 3), dependencies = emptyMap()),
        Pkg(
                name = "C",
                version = v(1, 0, 0),
                dependencies = mapOf(
                        "B" to r("1.0.1 <= v < 1.0.3")
                )
        ),
        Pkg(
                name = "D",
                version = v(1, 0, 0),
                dependencies = mapOf(
                        "B" to r("1.0.1 <= v < 1.0.2"),
                        "C" to r("1.0.0 <= v < 2.0.0")
                )
        )
)

class SolverTest {

    @Test
    fun `handles empty input`() = doTest(
            deps = emptyMap(),
            expect = emptyMap()
    )

    @Test
    fun `trivial resolve`() = doTest(
            deps = mapOf("C" to r("1.0.0 <= v < 2.0.0")),
            expect = mapOf(
                    "B" to v(1, 0, 2),
                    "C" to v(1, 0, 0)
            )
    )

    @Test
    fun `picks highest available version`() = doTest(
            deps = mapOf("B" to r("1.0.0 <= v < 2.0.0")),
            expect = mapOf("B" to v(1, 0, 3))
    )

    @Test
    fun `resolve mutual constraints`() = doTest(
            deps = mapOf(
                    "B" to r("1.0.0 <= v < 1.0.4"),
                    "C" to r("1.0.0 <= v < 2.0.0")
            ),
            expect = mapOf(
                    "B" to v(1, 0, 2),
                    "C" to v(1, 0, 0)
            ))

    @Test
    fun `resolve mutual constraints - multiple levels`() = doTest(
            deps = mapOf(
                    "B" to r("1.0.0 <= v < 1.0.4"),
                    "C" to r("1.0.0 <= v < 2.0.0"),
                    "D" to r("1.0.0 <= v < 2.0.0")
            ),
            expect = mapOf(
                    "B" to v(1, 0, 1),
                    "C" to v(1, 0, 0),
                    "D" to v(1, 0, 0)
            ))

    @Test
    fun `resolve extended constraints can be merged`() = doTest(
            deps = mapOf(
                    "A" to r("1.0.0 <= v < 2.0.0"),
                    "B" to r("1.0.0 <= v < 2.1.0")
            ),
            expect = mapOf(
                    "A" to v(1, 0, 0),
                    "B" to v(2, 0, 0)
            ),
            repo = Repository(
                    Pkg(
                            name = "A",
                            version = v(1, 0, 0),
                            dependencies = mapOf("B" to r("1.0.0 <= v < 3.0.0"))
                    ),
                    Pkg(name = "B", version = v(1, 0, 0), dependencies = emptyMap()),
                    Pkg(name = "B", version = v(2, 0, 0), dependencies = emptyMap()),
                    Pkg(name = "B", version = v(2, 1, 0), dependencies = emptyMap())
            )
    )

    @Test
    fun `resolve extended constraints cannot be merged - conflict on B`() = doTest(
            deps = mapOf(
                    "A" to r("1.0.0 <= v < 2.0.0"),
                    "B" to r("1.0.0 <= v < 2.0.0")
            ),
            expect = null,
            repo = Repository(
                    Pkg(
                            name = "A",
                            version = v(1, 0, 0),
                            dependencies = mapOf("B" to r("2.0.0 <= v < 3.0.0"))
                    ),
                    Pkg(name = "B", version = v(1, 0, 0), dependencies = emptyMap()),
                    Pkg(name = "B", version = v(2, 0, 0), dependencies = emptyMap())
            )
    )

    @Test
    fun `no solution because version constraint is lower than lowest version in repo`() = doTest(
            deps = mapOf("C" to r("0.0.0 <= v < 1.0.0")),
            expect = null)

    @Test
    fun `no solution because version constraint is higher than max version in repo`() = doTest(
            deps = mapOf("C" to r("9.0.0 <= v < 10.0.0")),
            expect = null)

    @Test
    fun `no solution because mutual constraints conflict with each other`() = doTest(
            deps = mapOf(
                    "B" to r("1.0.3 <= v < 1.0.4"),
                    "C" to r("1.0.0 <= v < 2.0.0")
            ),
            expect = null)
}

fun doTest(deps: Map<PkgName, Constraint>, expect: Map<PkgName, Version>?, repo: Repository = fixture) {
    assertEquals(expect, solve(deps, repo))
}

private fun r(str: String) = Constraint.parse(str)

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)