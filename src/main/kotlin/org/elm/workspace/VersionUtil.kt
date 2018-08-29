package org.elm.workspace

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer


// VERSION

@JsonDeserialize(using = VersionDeserializer::class)
data class Version(val x: Int, val y: Int, val z: Int) : Comparable<Version> {

    override fun toString() =
            "$x.$y.$z"

    override fun compareTo(other: Version) =
            when {
                x != other.x -> x.compareTo(other.x)
                y != other.y -> y.compareTo(other.y)
                z != other.z -> z.compareTo(other.z)
                else -> 0
            }

    companion object {
        val UNKNOWN: Version = Version(0, 0, 0)

        fun parse(text: String): Version {
            val parts = text.split(".")
            if (parts.size != 3) throw ParseException("expected a version number like '1.0.0', got '$text'")
            try {
                return Version(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } catch (e: NumberFormatException) {
                throw ParseException("expected a version number like '1.0.0', got '$text'")
            }
        }

        fun parseOrNull(text: String): Version? =
                try {
                    parse(text)
                } catch (e: ParseException) {
                    null
                }
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

        fun evaluate(left: Version, right: Version): Boolean {
            return when (this) {
                LESS_THAN -> left < right
                LESS_THAN_OR_EQUAL -> left <= right
            }
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

    fun contains(version: Version): Boolean {
        return (lowOp.evaluate(low, version) && highOp.evaluate(version, high))
    }

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