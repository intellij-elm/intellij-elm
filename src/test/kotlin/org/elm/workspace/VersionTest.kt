package org.elm.workspace

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VersionTest {

    @Test
    fun `version compare`() {
        assertTrue(v(1, 0, 0) < v(2, 0, 0))
        assertTrue(v(2, 0, 0) > v(1, 0, 0))
        assertTrue(v(1, 0, 0) == v(1, 0, 0))

        assertTrue(v(0, 1, 0) < v(0, 2, 0))
        assertTrue(v(0, 2, 0) > v(0, 1, 0))
        assertTrue(v(0, 1, 0) == v(0, 1, 0))

        assertTrue(v(0, 0, 1) < v(0, 0, 2))
        assertTrue(v(0, 0, 2) > v(0, 0, 1))
        assertTrue(v(0, 0, 1) == v(0, 0, 1))
    }

    @Test
    fun `version toString is dotted form`() {
        assertEquals("1.2.3", v(1, 2, 3).toString())
    }

    @Test()
    fun `parse works on good input`() {
        Version.parse("1.2.3")
    }

    @Test()
    fun `toString emits the dotted version number without any adornment`() {
        assertEquals("1.2.3", Version.parse("1.2.3").toString())
    }

    @Test(expected = ParseException::class)
    fun `parse throws on bad input`() {
        Version.parse("bogus.version.number")
    }

    @Test()
    fun `parseOrNull does not throw on bad input`() {
        assertNull(Version.parseOrNull("bogus.version.number"))
    }

    private fun v(x: Int, y: Int, z: Int) =
            Version(x, y, z)
}