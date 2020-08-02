package org.elm.workspace.solver

import org.elm.workspace.Constraint
import org.elm.workspace.Version
import org.elm.workspace.solver.SolverResult.DeadEnd
import org.elm.workspace.solver.SolverResult.Proceed

typealias PkgName = String

data class Pkg(
        val name: PkgName,
        val version: Version,
        val elmVersion: Constraint = elm19, // TODO: get rid of the default outside of test cases
        val dependencies: Map<PkgName, Constraint>
)

fun Version.satisfies(constraint: Constraint): Boolean = constraint.contains(this)

private val elm19 = Constraint.parse("0.19.0 <= v < 0.20.0")

data class Repository(private val packagesByName: Map<PkgName, List<Pkg>>) {
    constructor(vararg packages: Pkg) : this(packages.groupBy { it.name })

    operator fun get(name: String): List<Pkg> {
        return packagesByName[name] ?: emptyList()
    }
}

sealed class SolverResult {
    object DeadEnd : SolverResult()
    data class Proceed(
            val pending: Map<PkgName, Constraint>,
            val solutions: Map<PkgName, Version>
    ) : SolverResult()
}

fun solve(deps: Map<PkgName, Constraint>, repo: Repository): Map<PkgName, Version>? {
    val solutions = mapOf<PkgName, Version>()
    return when (val res = Solver(repo).solve(deps, solutions)) {
        DeadEnd -> null
        is Proceed -> res.solutions
    }
}

class Solver(private val repo: Repository) {
    fun solve(deps: Map<PkgName, Constraint>, solutions: Map<PkgName, Version>): SolverResult {
        // Pick a dep/constraint (Elm compiler picks by names in lexicographically ascending order)
        val dep = deps.minBy { it.key }
        if (dep == null) {
            // no more dependencies left to explore
            return Proceed(deps, solutions)
        }

        val (pkgName, constraint) = dep

        // Figure out which versions of the dep actually exist
        val candidates = repo[pkgName].filter { it.version.satisfies(constraint) }
        println("$pkgName has candidates ${candidates.map { it.version }}")

        if (candidates.isEmpty()) {
            return DeadEnd
        }

        for (candidate in candidates) {
        }

        return DeadEnd
    }
}



