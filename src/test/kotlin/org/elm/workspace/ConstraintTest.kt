package org.elm.workspace

import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertNull

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
    fun `can determine whether a version satisfies a SemVer constraint`() {
        val c = Constraint(
                low = v(1, 0, 0),
                high = v(2, 0, 0),
                lowOp = Constraint.Op.LESS_THAN_OR_EQUAL,
                highOp = Constraint.Op.LESS_THAN
        )
        assertFalse(c.contains(Version(0, 9, 0)))
        assertFalse(c.contains(Version(0, 9, 0, preReleaseFields = listOf("alpha"))))
        assertTrue(c.contains(Version(1, 0, 0)))
        assertTrue(c.contains(Version(1, 0, 0, preReleaseFields = listOf("alpha"))))
        assertTrue(c.contains(Version(1, 1, 0)))
        assertTrue(c.contains(Version(1, 1, 0, preReleaseFields = listOf("alpha"))))
        assertFalse(c.contains(Version(2, 0, 0)))
        assertFalse(c.contains(Version(2, 0, 0, preReleaseFields = listOf("alpha"))))
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

    @Test
    fun `empty intersection`() {
        assertNull(c("1.0.0 <= v < 1.0.1") intersect c("1.0.1 <= v < 2.0.0"))
        assertNull(c("1.0.0 <= v < 1.1.0") intersect c("1.1.0 <= v < 2.0.0"))
        assertNull(c("1.0.0 <= v < 2.0.0") intersect c("2.0.0 <= v < 3.0.0"))
    }

    @Test
    fun `non-empty intersections`() {
        assertEquals(c("1.0.0 <= v < 2.0.0"), c("1.0.0 <= v < 2.0.0") intersect c("1.0.0 <= v < 3.0.0"))
        assertEquals(c("2.0.0 <= v < 3.0.0"), c("1.0.0 <= v < 3.0.0") intersect c("2.0.0 <= v < 3.0.0"))
        assertEquals(c("2.0.0 <= v < 3.0.0"), c("1.0.0 <= v < 3.0.0") intersect c("2.0.0 <= v < 4.0.0"))

        assertEquals(c("1.0.0 <= v < 1.1.0"), c("1.0.0 <= v < 2.0.0") intersect c("1.0.0 <= v < 1.1.0"))
        assertEquals(c("1.2.0 <= v < 1.3.0"), c("1.0.0 <= v < 1.3.0") intersect c("1.2.0 <= v < 1.4.0"))

        assertEquals(c("1.2.3 <= v < 1.3.0"), c("1.2.3 <= v < 2.0.0") intersect c("1.1.1 <= v < 1.3.0"))
    }
}

private fun c(str: String) =
        Constraint.parse(str)

private fun v(x: Int, y: Int, z: Int) =
        Version(x, y, z)