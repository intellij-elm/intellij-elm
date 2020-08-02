package org.elm.workspace.solver

import org.elm.workspace.Constraint
import org.elm.workspace.Version
import org.junit.Assert.assertEquals
import org.junit.Test

private val repo = Repository(
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
        )
)

class SolverTest {

    @Test
    fun `trivial resolve`() = doTest(
            deps = mapOf("C" to r("1.0.0 <= v < 2.0.0")),
            expect = mapOf("C" to v(1, 0, 0)))

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

fun doTest(deps: Map<PkgName, Constraint>, expect: Map<PkgName, Version>?) {
    assertEquals(expect, solve(deps, repo))
}

private fun r(str: String) = Constraint.parse(str)

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)