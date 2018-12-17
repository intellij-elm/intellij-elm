package org.elm.workspace

import org.junit.Assert.*
import org.junit.Test

class ConstraintTest {

    @Test
    fun `can determine whether a version satisfies a half-open constraint`() {
        val c = Constraint(
                low = v(1, 0, 0),
                high = v(2, 0, 0),
                lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
                highOp = Constraint.Op.LESS_THAN
        )
        assertFalse(c.contains(v(0, 9, 0)))
        assertTrue(c.contains(v(1, 0, 0)))
        assertTrue(c.contains(v(1, 1, 0)))
        assertFalse(c.contains(v(2, 0, 0)))
    }

    @Test
    fun `can determine whether a version satisfies an inclusive constraint`() {
        val c = Constraint(
                low = v(1, 0, 0),
                high = v(2, 0, 0),
                lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
                highOp = Constraint.Op.LESS_THAN_OR_EQUAL
        )
        assertFalse(c.contains(v(0, 9, 0)))
        assertTrue(c.contains(v(1, 0, 0)))
        assertTrue(c.contains(v(1, 1, 0)))
        assertTrue(c.contains(v(2, 0, 0)))
        assertFalse(c.contains(v(2, 1, 0)))
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

    @Test
    fun `toString emits a readable string`() {
        assertEquals("1.0.0 <= v < 2.0.0", Constraint.parse("1.0.0 <= v < 2.0.0").toString())
    }
}

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)