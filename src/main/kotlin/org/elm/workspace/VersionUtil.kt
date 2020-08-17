package org.elm.workspace

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer


/**
 * A version number according to the [SemVer spec](https://semver.org)
 */
@JsonDeserialize(using = VersionDeserializer::class)
data class Version(
        val x: Int,
        val y: Int,
        val z: Int,
        val preReleaseFields: List<String> = emptyList(),
        val buildFields: List<String> = emptyList()
) : Comparable<Version> {

    override fun toString(): String {
        val str = StringBuilder("$x.$y.$z")
        if (preReleaseFields.isNotEmpty()) str.append(preReleaseFields.joinToString(".", prefix = "-"))
        if (buildFields.isNotEmpty()) str.append(buildFields.joinToString(".", prefix = "+"))
        return str.toString()
    }

    override fun compareTo(other: Version) =
            when {
                x != other.x -> x.compareTo(other.x)
                y != other.y -> y.compareTo(other.y)
                z != other.z -> z.compareTo(other.z)
                preReleaseFields != other.preReleaseFields -> compareSemVerFields(preReleaseFields, other.preReleaseFields)
                else -> 0
            }

    /**
     * Returns a "loose" form that ignores suffixes like alpha, rc-1, etc.
     */
    val xyz: Version by lazy { Version(x, y, z) }

    companion object {
        val UNKNOWN: Version = Version(0, 0, 0)

        /** A weak definition of the version format defined in [the SemVer spec](https://semver.org) */
        private val PATTERN = Regex("""(\d+)\.(\d+)\.(\d+)(-[0-9A-Za-z\-.]+)?(\+[0-9A-Za-z\-.]+)?""")

        fun parse(text: String): Version {
            val result = PATTERN.find(text) ?: throw ParseException("expected a version number, got '$text'")
            val (x, y, z, preReleaseInfo, buildInfo) = result.destructured
            return Version(x.toInt(), y.toInt(), z.toInt(),
                    preReleaseFields = preReleaseInfo.parseExtraParts(),
                    buildFields = buildInfo.parseExtraParts()
            )
        }

        private fun String.parseExtraParts(): List<String> =
                takeIf { it.isNotEmpty() }?.drop(1)?.split(".") ?: emptyList()

        fun parseOrNull(text: String): Version? =
                try {
                    parse(text)
                } catch (e: ParseException) {
                    null
                }
    }
}

private fun compareSemVerFields(leftFields: List<String>, rightFields: List<String>): Int {
    // First, handle differences between normal versions and pre-release versions.
    // A normal version always has higher precedence than its pre-release versions.
    if (leftFields.isEmpty()) return 1
    if (rightFields.isEmpty()) return -1

    // Compare fields pair-wise
    for ((a, b) in leftFields.zip(rightFields)) {
        val result = compareSemVerField(a, b)
        if (result != 0) return result
    }

    // All pair-wise fields are the same: tie-breaker on number of fields
    return leftFields.size.compareTo(rightFields.size)
}


private fun compareSemVerField(a: String, b: String): Int {
    val a2 = a.toIntOrNull()
    val b2 = b.toIntOrNull()
    return when {
        a2 == null && b2 == null -> a.compareTo(b)   // both fields are strings
        a2 == null && b2 != null -> 1                // numeric field has lower precedence than non-numeric field
        a2 != null && b2 == null -> -1               // ditto
        a2 != null && b2 != null -> a2.compareTo(b2) // both fields are integers
        else -> error("cannot happen")
    }
}


private class VersionDeserializer : StdDeserializer<Version>(Version::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
            try {
                Version.parse(p.text)
            } catch (e: ParseException) {
                throw ctxt.weirdStringException(p.text, Version::class.java, e.message)
            }
}


// CONSTRAINT ON VERSION NUMBERS

@JsonDeserialize(using = ConstraintDeserializer::class)
data class Constraint(
        val low: Version,
        val high: Version,
        val lowOp: Op,
        val highOp: Op
) {
    enum class Op {
        LESS_THAN,
        LESS_THAN_OR_EQUAL;

        override fun toString(): String =
                when (this) {
                    LESS_THAN -> "<"
                    LESS_THAN_OR_EQUAL -> "<="
                }

        fun evaluate(left: Version, right: Version): Boolean =
                when (this) {
                    LESS_THAN -> left < right
                    LESS_THAN_OR_EQUAL -> left <= right
                }

        companion object {
            fun parse(text: String): Op =
                    when (text) {
                        "<" -> LESS_THAN
                        "<=" -> LESS_THAN_OR_EQUAL
                        else -> throw ParseException("expected '<' or '<=', got '$text'")
                    }
        }
    }

    /**
     * Returns true if the constraint is satisfied using SemVer ordering (namely "1.0-beta" < "1.0")
     */
    fun semVerContains(version: Version): Boolean =
            (lowOp.evaluate(low, version) && highOp.evaluate(version, high))

    /**
     * Returns true if the constraint is satisfied solely by comparing x.y.z (so "1.0-beta" == "1.0")
     */
    operator fun contains(version: Version): Boolean =
            copy(low = low.xyz, high = high.xyz).semVerContains(version.xyz)

    /**
     * Returns the intersection with [other] or null if the intersection is empty.
     */
    infix fun intersect(other: Constraint): Constraint? {
        fun merge(op1: Op, op2: Op) =
                if (Op.LESS_THAN in listOf(op1, op2)) Op.LESS_THAN else Op.LESS_THAN_OR_EQUAL

        val (newLo, newLop) = when (low.compareTo(other.low)) {
            -1 -> other.low to other.lowOp
            0 -> low to merge(lowOp, other.lowOp)
            1 -> low to lowOp
            else -> error("unexpected compare result")
        }

        val (newHi, newHop) = when (high.compareTo(other.high)) {
            -1 -> high to highOp
            0 -> high to merge(highOp, other.highOp)
            1 -> other.high to other.highOp
            else -> error("unexpected compare result")
        }

        if (newLo >= newHi) return null
        return Constraint(newLo, newHi, newLop, newHop)
    }

    override fun toString() =
            "$low $lowOp v $highOp $high"

    companion object {
        fun parse(text: String): Constraint {
            val parts = text.split(" ")
            if (parts.size != 5) throw ParseException("expected something like '1.0.0 <= v < 2.0.0', got '$text'")
            val low = Version.parse(parts[0])
            val lowOp = Op.parse(parts[1])
            if (parts[2] != "v") throw ParseException("expected 'v', got '${parts[2]}'")
            val highOp = Op.parse(parts[3])
            val high = Version.parse(parts[4])
            return Constraint(low = low, lowOp = lowOp, highOp = highOp, high = high)
        }
    }
}

private class ConstraintDeserializer : StdDeserializer<Constraint>(Constraint::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
            try {
                Constraint.parse(p.text)
            } catch (e: ParseException) {
                throw ctxt.weirdStringException(p.text, Constraint::class.java, e.message)
            }
}


// MISC

class ParseException(msg: String) : RuntimeException(msg)