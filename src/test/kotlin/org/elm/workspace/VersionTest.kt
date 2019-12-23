package org.elm.workspace

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `version compare takes into account pre-release info`() {
        // based on example ordering from the SemVer spec
        val versionsInOrder = listOf(
                "1.0.0-alpha.1",
                "1.0.0-alpha.beta",
                "1.0.0-beta",
                "1.0.0-beta.2",
                "1.0.0-beta.11",
                "1.0.0-rc.1",
                "1.0.0"
        )

        versionsInOrder.zipWithNext { a: String, b: String ->
            assertTrue("$a < $b") {
                Version.parse(a) < Version.parse(b)
            }
        }
    }

    @Test
    fun `version compare ignores build metadata`() {
        assertTrue(Version.parse("1.0.0+foo").compareTo(Version.parse("1.0.0+bar")) == 0)
    }

    @Test
    fun `version toString is dotted form`() {
        assertEquals("1.2.3", v(1, 2, 3).toString())
    }

    @Test
    fun `version toString includes pre-release info if available`() {
        assertEquals("1.2.3-rc1", Version(1, 2, 3, preReleaseFields = listOf("rc1")).toString())
    }

    @Test
    fun `parse works on good input`() {
        Version.parse("1.2.3")
    }

    @Test
    fun `parse ignores junk around the version number`() {
        assertEquals(Version.parse("foo 1.2.3 bar"), Version(1, 2, 3))
    }

    @Test
    fun `parses pre-release version info`() {
        assertEquals(
                Version.parse("1.2.3-alpha"),
                Version(1, 2, 3, preReleaseFields = listOf("alpha")))
    }

    @Test
    fun `parses build metadata`() {
        assertEquals(
                Version.parse("1.2.3+99999"),
                Version(1, 2, 3, buildFields = listOf("99999")))
    }

    @Test
    fun `parses pre-release version info AND build metadata`() {
        assertEquals(
                Version.parse("1.2.3-alpha+99999"),
                Version(1, 2, 3, preReleaseFields = listOf("alpha"), buildFields = listOf("99999")))
    }


    @Test
    fun `toString emits the dotted version number without any adornment`() {
        assertEquals("1.2.3", Version.parse("1.2.3").toString())
    }

    @Test(expected = ParseException::class)
    fun `parse throws on bad input`() {
        Version.parse("bogus.version.number")
    }

    @Test
    fun `parseOrNull does not throw on bad input`() {
        assertNull(Version.parseOrNull("bogus.version.number"))
    }

    private fun v(x: Int, y: Int, z: Int) =
            Version(x, y, z)
}