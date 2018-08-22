package org.elm.workspace

import org.junit.Assert.*
import org.junit.Test

class ConstraintTest {

    @Test
    fun `can determine whether a version satisfies a constraint`() {
        val c = Constraint(
                low = v(1, 0, 0),
                high = v(2, 0, 0),
                lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
                highOp = Constraint.Op.LESS_THAN
        )
        assertTrue(c.contains(v(1, 0, 0)))
        assertTrue(c.contains(v(1, 1, 0)))
        assertFalse(c.contains(v(2, 0, 0)))
    }

    @Test
    fun `parse works on good input`() {
        assertEquals(Constraint(
                low = v(1, 0, 0),
                high = v(2, 0, 0),
                lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
                highOp = Constraint.Op.LESS_THAN
        ), Constraint.parse("1.0.0 <= v < 2.0.0"))
    }

    @Test(expected = ParseException::class)
    fun `parse throws on bad input`() {
        Constraint.parse("1.0.0 <= v <= bogus")
    }
}

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)