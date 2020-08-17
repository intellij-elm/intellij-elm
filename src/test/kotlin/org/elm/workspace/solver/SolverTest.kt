package org.elm.workspace.solver

import org.elm.workspace.Constraint
import org.elm.workspace.Version
import org.junit.Assert.assertEquals
import org.junit.Test

fun compilerConstraint(v: Int) = Constraint.parse("0.${v}.0 <= v < 0.${v + 1}.0")

data class SimplePkg(
        override val name: PkgName,
        override val version: Version,
        override val dependencies: Map<PkgName, Constraint>,
        override val elmConstraint: Constraint = compilerConstraint(19)
) : Pkg

data class SimpleRepository(private val packagesByName: Map<PkgName, List<Pkg>>) : Repository {
    override var elmCompilerVersion: Version = v(0, 19, 1)

    constructor(vararg packages: Pkg) : this(packages.groupBy { it.name })

    override operator fun get(name: String): List<Pkg> {
        return packagesByName[name] ?: emptyList()
    }
}

private val fixture = SimpleRepository(
        SimplePkg(name = "B", version = v(1, 0, 0), dependencies = emptyMap()),
        SimplePkg(name = "B", version = v(1, 0, 1), dependencies = emptyMap()),
        SimplePkg(name = "B", version = v(1, 0, 2), dependencies = emptyMap()),
        SimplePkg(name = "B", version = v(1, 0, 3), dependencies = emptyMap()),
        SimplePkg(
                name = "C",
                version = v(1, 0, 0),
                dependencies = mapOf(
                        "B" to r("1.0.1 <= v < 1.0.3")
                )
        ),
        SimplePkg(
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
            repo = SimpleRepository(
                    SimplePkg(
                            name = "A",
                            version = v(1, 0, 0),
                            dependencies = mapOf("B" to r("1.0.0 <= v < 3.0.0"))
                    ),
                    SimplePkg(name = "B", version = v(1, 0, 0), dependencies = emptyMap()),
                    SimplePkg(name = "B", version = v(2, 0, 0), dependencies = emptyMap()),
                    SimplePkg(name = "B", version = v(2, 1, 0), dependencies = emptyMap())
            )
    )

    @Test
    fun `resolve extended constraints cannot be merged - conflict on B`() = doTest(
            deps = mapOf(
                    "A" to r("1.0.0 <= v < 2.0.0"),
                    "B" to r("1.0.0 <= v < 2.0.0")
            ),
            expect = null,
            repo = SimpleRepository(
                    SimplePkg(
                            name = "A",
                            version = v(1, 0, 0),
                            dependencies = mapOf("B" to r("2.0.0 <= v < 3.0.0"))
                    ),
                    SimplePkg(name = "B", version = v(1, 0, 0), dependencies = emptyMap()),
                    SimplePkg(name = "B", version = v(2, 0, 0), dependencies = emptyMap())
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

    @Test
    fun `no solution because Elm compiler version requirement cannot be satisfied`() = doTest(
            deps = mapOf("A" to r("1.0.0 <= v < 2.0.0")),
            expect = null,
            repo = SimpleRepository(
                    SimplePkg("A", v(1, 0, 0), emptyMap(), compilerConstraint(18))
            ).apply { elmCompilerVersion = v(0, 19, 0) }
    )
}

fun doTest(deps: Map<PkgName, Constraint>, expect: Map<PkgName, Version>?, repo: Repository = fixture) {
    assertEquals(expect, solve(deps, repo))
}

private fun r(str: String) = Constraint.parse(str)

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)