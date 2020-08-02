package org.elm.workspace.solver

import org.elm.workspace.Constraint
import org.elm.workspace.Version
import org.junit.Assert.assertEquals
import org.junit.Test

typealias PkgName = String

data class Pkg(
        val name: PkgName,
        val version: Version,
        val elmVersion: Constraint = elm19, // TODO: get rid of the default outside of test cases
        val dependencies: Map<PkgName, Constraint>
)

data class Repository(val packagesByName: Map<PkgName, List<Pkg>>) {
    constructor(vararg packages: Pkg) : this(packages.groupBy { it.name })
}

fun solve(dependencies: Map<PkgName, Constraint>, repository: Repository): Map<PkgName, Version> {
    return dependencies.mapValues { (name, constraint) ->
        (repository.packagesByName[name] ?: error("package $name not found in repository"))
                .map { it.version }
                .filter { constraint.contains(it) }
                .max()
                ?: error("package $name does not have a version in repo that satisfies constraint $constraint")
    }
}

private val elm19 = Constraint.parse("0.19.0 <= v < 0.20.0")

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
    fun `trivial resolve`() {
        val input = mapOf(
                "C" to r("1.0.0 <= v < 2.0.0")
        )

        assertEquals(solve(input, repo), mapOf(
                "C" to v(1, 0, 0)
        ))
    }

    @Test
    fun `picks highest available version`() {
        val input = mapOf(
                "B" to r("1.0.0 <= v < 2.0.0")
        )

        assertEquals(solve(input, repo), mapOf(
                "B" to v(1, 0, 3)
        ))
    }

    @Test
    fun `resolve mutual constraints`() {
        val input = mapOf(
                "B" to r("1.0.0 <= v < 1.0.4"),
                "C" to r("1.0.0 <= v < 2.0.0")
        )

        assertEquals(solve(input, repo), mapOf(
                "B" to v(1, 0, 2),
                "C" to v(1, 0, 0)
        ))
    }
}

private fun r(str: String) = Constraint.parse(str)

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)