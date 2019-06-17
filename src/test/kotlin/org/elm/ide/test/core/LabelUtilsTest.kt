package org.elm.ide.test.core

import com.intellij.openapi.vfs.VirtualFileManager.extractPath
import org.elm.ide.test.core.LabelUtils.commonParent
import org.elm.ide.test.core.LabelUtils.fromErrorLocationUrlPath
import org.elm.ide.test.core.LabelUtils.fromLocationUrlPath
import org.elm.ide.test.core.LabelUtils.pathString
import org.elm.ide.test.core.LabelUtils.subParents
import org.elm.ide.test.core.LabelUtils.toErrorLocationUrl
import org.elm.ide.test.core.LabelUtils.toPath
import org.elm.ide.test.core.LabelUtils.toSuiteLocationUrl
import org.elm.ide.test.core.LabelUtils.toTestLocationUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class LabelUtilsTest {

    @Test
    fun locationUrl() {
        val url = toTestLocationUrl(toPath(Arrays.asList("Module", "test")))
        assertEquals("elmTestTest://Module/test", url)
    }

    @Test
    fun suiteLocationUrl() {
        val url = toSuiteLocationUrl(toPath(Arrays.asList("Module", "suite")))
        assertEquals("elmTestDescribe://Module/suite", url)
    }

    @Test
    fun locationUrlWithSlash() {
        val url = toTestLocationUrl(toPath(Arrays.asList("Nested.Module", "test / stuff")))
        assertEquals("elmTestTest://Nested.Module/test+%2F+stuff", url)
    }

    @Test
    fun useLocationUrl() {
        val url = toTestLocationUrl(toPath(Arrays.asList("Nested.Module", "test")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Nested/Module.elm", pair.first)
        assertEquals("test", pair.second)
    }

    @Test
    fun useNestedLocationUrl() {
        val url = toTestLocationUrl(toPath(Arrays.asList("Nested.Module", "suite", "test")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Nested/Module.elm", pair.first)
        assertEquals("suite/test", pair.second)
    }

    @Test
    fun useLocationUrlWithSlash() {
        val url = toTestLocationUrl(toPath(Arrays.asList("Module", "test / stuff")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Module.elm", pair.first)
        assertEquals("test / stuff", pair.second)
    }

    @Test
    fun useLocationModuleOnly() {
        val url = toTestLocationUrl(toPath(listOf("Module")))
        val urlPath = url.substring(url.indexOf("://") + 3)

        val pair = fromLocationUrlPath(urlPath)
        assertEquals("tests/Module.elm", pair.first)
        assertEquals("", pair.second)
    }

    @Test
    fun commonParentSameSuite() {
        val from = toPath(Arrays.asList("Module", "suite", "test"))
        val to = toPath(Arrays.asList("Module", "suite", "test2"))

        val parent = commonParent(from, to)
        assertEquals("Module/suite", pathString(parent))
    }

    @Test
    fun commonParentDifferentSuite() {
        val from = toPath(Arrays.asList("Module", "suite", "test"))
        val to = toPath(Arrays.asList("Module", "suite2", "test2"))

        val parent = commonParent(from, to)
        assertEquals("Module", pathString(parent))

        val parent2 = commonParent(to, from)
        assertEquals("Module", pathString(parent2))
    }

    @Test
    fun commonParentDifferentSuite2() {
        val from = toPath(Arrays.asList("Module", "suite", "deep", "test"))
        val to = toPath(Arrays.asList("Module", "suite2", "test2"))

        val parent = commonParent(from, to)
        assertEquals("Module", pathString(parent))

        val parent2 = commonParent(to, from)
        assertEquals("Module", pathString(parent2))
    }

    @Test
    fun commonParentNoParent() {
        val from = toPath(Arrays.asList("Module", "suite", "test"))
        val to = toPath(Arrays.asList("Module2", "suite2", "test2"))

        val parent = commonParent(from, to)
        assertEquals("", pathString(parent))
    }

    @Test
    fun parentPaths() {
        val path = toPath(Arrays.asList("Module", "suite", "test"))
        val parent = toPath(listOf("Module"))

        val parents = subParents(path, parent)
                .asSequence()
                .map { pathString(it) }
                .toList()

        assertEquals(listOf("Module/suite"), parents)
    }

    @Test
    fun errorLocationUrl() {
        val url = toErrorLocationUrl("my/path/file", 1313, 13)
        assertEquals("elmTestError://my/path/file::1313::13", url)

        val path = extractPath(url)
        val pair = fromErrorLocationUrlPath(path)

        val file = pair.first
        val line = pair.second.first
        val column = pair.second.second

        assertEquals("my/path/file", file)
        assertEquals(1313, line.toLong())
        assertEquals(13, column.toLong())
    }

}