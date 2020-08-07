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

fun solve(deps: Map<PkgName, Constraint>, repo: Repository): Map<PkgName, Version>? {
    val solutions = mapOf<PkgName, Version>()
    return when (val res = Solver(repo).solve(deps, solutions)) {
        DeadEnd -> null
        is Proceed -> res.solutions
    }
}

private sealed class SolverResult {
    object DeadEnd : SolverResult()
    data class Proceed(
            val pending: Map<PkgName, Constraint>,
            val solutions: Map<PkgName, Version>
    ) : SolverResult()
}

private class Solver(private val repo: Repository) {
    fun solve(deps: Map<PkgName, Constraint>, solutions: Map<PkgName, Version>): SolverResult {
        // Pick a dep to solve
        val (dep, restDeps) = deps.pick()
        if (dep == null) {
            // Base case: no more dependencies left to explore
            return Proceed(deps, solutions)
        }

        // Figure out which versions of the dep actually exist
        var candidates = repo[dep.name]
                .filter { it.version.satisfies(dep.constraint) }
                .sortedByDescending { it.version }
        println("${dep.name} has candidates ${candidates.map { it.version }}")

        // Further restrict the candidates if a pending solution has already bound the version of this dep.
        val solvedVersion = solutions[dep.name]
        if (solvedVersion != null) {
            if (candidates.any { it.version == solvedVersion }) {
                // TODO [kl] shouldn't this never happen? or if it does, we should be able to immediately move on.
                println("Already solved ${dep.name} as $solvedVersion")
            }
            candidates = candidates.filter { it.version == solvedVersion }
        }

        if (candidates.isEmpty()) {
            return DeadEnd
        }

        // Speculatively try each candidate version to see if it is a partial solution
        loop@ for (candidate in candidates) {
            // TODO [kl] check that constraints for the same key are compatible when combining
            val tentativeDeps = restDeps + candidate.dependencies

            // TODO [kl] check that solutions for the same key are equal when combining
            val tentativeSolutions = solutions + (dep.name to candidate.version)

            // TODO [kl] are we going to overflow the stack when recursing?
            when (val res = solve(tentativeDeps, tentativeSolutions)) {
                DeadEnd -> continue@loop
                is Proceed -> return solve(res.pending, res.solutions)
            }
        }

        return DeadEnd
    }
}


private data class Dep(val name: PkgName, val constraint: Constraint)

private fun Map<PkgName, Constraint>.pick(): Pair<Dep?, Map<PkgName, Constraint>> {
    // Pick a dep/constraint (Elm compiler picks by pkg name in lexicographically ascending order)
    val dep = minBy { it.key } ?: return (null to this)
    return Dep(dep.key, dep.value) to minus(dep.key)
}
