package org.elm.ide.test.core

import com.intellij.openapi.vfs.VirtualFileManager.extractPath
import org.elm.ide.test.core.LabelUtils.commonParent
import org.elm.ide.test.core.LabelUtils.fromLocationUrlPath
import org.elm.ide.test.core.LabelUtils.pathString
import org.elm.ide.test.core.LabelUtils.subParents
import org.elm.ide.test.core.LabelUtils.toLocationUrl
import org.elm.ide.test.core.LabelUtils.toPath
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelUtilsTest {

    @Test
    fun locationUrl() {
        val url = toLocationUrl(toPath(listOf("Module", "test")))
        assertEquals("elmTestTest://Module/test", url)
    }

    @Test
    fun suiteLocationUrl() {
        val url = toLocationUrl(toPath(listOf("Module", "test")), isSuite = true)
        assertEquals("elmTestDescribe://Module/test", url)
    }

    @Test
    fun locationUrlWithSlash() {
        val url = toLocationUrl(toPath(listOf("Nested.Module", "test / stuff")))
        assertEquals("elmTestTest://Nested.Module/test+%2F+stuff", url)
    }

    @Test
    fun useLocationUrl() {
        val url = toLocationUrl(toPath(listOf("Nested.Module", "test")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Nested/Module.elm", pair.first)
        assertEquals("test", pair.second)
    }

    @Test
    fun useNestedLocationUrl() {
        val url = toLocationUrl(toPath(listOf("Nested.Module", "suite", "test")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Nested/Module.elm", pair.first)
        assertEquals("suite/test", pair.second)
    }

    @Test
    fun useLocationUrlWithSlash() {
        val url = toLocationUrl(toPath(listOf("Module", "test / stuff")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Module.elm", pair.first)
        assertEquals("test / stuff", pair.second)
    }

    @Test
    fun useLocationModuleOnly() {
        val url = toLocationUrl(toPath(listOf("Module")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Module.elm", pair.first)
        assertEquals("", pair.second)
    }

    @Test
    fun commonParentSameSuite() {
        val from = toPath(listOf("Module", "suite", "test"))
        val to = toPath(listOf("Module", "suite", "test2"))

        val parent = commonParent(from, to)
        assertEquals("Module/suite", pathString(parent))
    }

    @Test
    fun commonParentDifferentSuite() {
        val from = toPath(listOf("Module", "suite", "test"))
        val to = toPath(listOf("Module", "suite2", "test2"))

        val parent = commonParent(from, to)
        assertEquals("Module", pathString(parent))

        val parent2 = commonParent(to, from)
        assertEquals("Module", pathString(parent2))
    }

    @Test
    fun commonParentDifferentSuite2() {
        val from = toPath(listOf("Module", "suite", "deep", "test"))
        val to = toPath(listOf("Module", "suite2", "test2"))

        val parent = commonParent(from, to)
        assertEquals("Module", pathString(parent))

        val parent2 = commonParent(to, from)
        assertEquals("Module", pathString(parent2))
    }

    @Test
    fun commonParentNoParent() {
        val from = toPath(listOf("Module", "suite", "test"))
        val to = toPath(listOf("Module2", "suite2", "test2"))

        val parent = commonParent(from, to)
        assertEquals("", pathString(parent))
    }

    @Test
    fun parentPaths() {
        val path = toPath(listOf("Module", "suite", "test"))
        val parent = toPath(listOf("Module"))

        val parents = subParents(path, parent).toList().map { pathString(it) }

        assertEquals(listOf("Module/suite"), parents)
    }

    @Test
    fun errorLocationUrl() {
        val url = ErrorLabelLocation("my/path/file", 1313, 13).toUrl()
        assertEquals("elmTestError://my/path/file::1313::13", url)

        val path = extractPath(url)
        val location = ErrorLabelLocation.fromUrl(path)

        assertEquals("my/path/file", location.file)
        assertEquals(1313, location.line)
        assertEquals(13, location.column)
    }

}