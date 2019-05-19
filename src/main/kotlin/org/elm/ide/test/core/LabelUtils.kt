package org.elm.ide.test.core

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream

object LabelUtils {
    val ELM_TEST_PROTOCOL = "elmTest"
    val DESCRIBE_PROTOCOL = ELM_TEST_PROTOCOL + "Describe"
    private val TEST_PROTOCOL = ELM_TEST_PROTOCOL + "Test"
    val ERROR_PROTOCOL = ELM_TEST_PROTOCOL + "Error"

    //internal
    val EMPTY_PATH = Paths.get("")

    private fun getModuleName(path: Path): String {
        return pathString(path.getName(0))
    }

    private fun encodeLabel(label: String): String {
        try {
            return URLEncoder.encode(label, "utf8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }

    }

    //internal
    fun decodeLabel(encoded: Path): String {
        try {
            return URLDecoder.decode(pathString(encoded), "utf8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }

    }

    //internal
    fun toPath(labels: List<String>): Path {
        val encoded = labels
                .asSequence()
                .map { encodeLabel(it) }

        return if (encoded.count() < 1) {
            EMPTY_PATH
        } else {
            Paths.get(
                    encoded.first(),
                    *encoded.drop(1).toList().toTypedArray())
        }
    }

    //internal
    fun pathString(path: Path): String {
        return FileUtil.toSystemIndependentName(path.toString())
    }

    fun getName(path: Path): String {
        return decodeLabel(path.fileName)
    }

    //internal
    fun toSuiteLocationUrl(path: Path): String {
        return toLocationUrl(DESCRIBE_PROTOCOL, path)
    }

    //internal
    fun toTestLocationUrl(path: Path): String {
        return toLocationUrl(TEST_PROTOCOL, path)
    }

    private fun toLocationUrl(protocol: String, path: Path): String {
        return String.format("%s://%s", protocol, pathString(path))
    }

    fun fromLocationUrlPath(path: String): Pair<String, String> {
        val path1 = Paths.get(path)
        val moduleName = getModuleName(path1)
        val moduleFile = String.format("tests/%s.elm", moduleName.replace(".", "/"))
        val label = if (path1.nameCount > 1) decodeLabel(path1.subpath(1, path1.nameCount)) else ""
        return Pair(moduleFile, label)
    }

    //internal
    fun commonParent(path1: Path?, path2: Path): Path {
        if (path1 == null) {
            return EMPTY_PATH
        }
        if (path1.nameCount > path2.nameCount) {
            return commonParent(path2, path1)
        }
        return if (path2.startsWith(path1)) {
            path1
        } else {
            commonParent(path1.parent, path2)
        }
    }

    //internal
    fun subParents(path: Path, excludeParent: Path): Stream<Path> {
        if (excludeParent === EMPTY_PATH) {
            // TODO remove duplication with below
            val result = ArrayList<Path>()
            var current: Path? = path.parent
            while (current != null) {
                result.add(current)
                current = current.parent
            }
            return result.stream()
        }

        if (!path.startsWith(excludeParent)) {
            throw IllegalStateException("not parent")
        }

        if (path === EMPTY_PATH) {
            return Stream.empty()
        }

        val result = ArrayList<Path>()
        var current = path.parent
        while (current != excludeParent) {
            result.add(current)
            current = current.parent
        }
        return result.stream()

        // JSK 9
        //        return Stream.iterate(path, current -> current != null ? current.getParent() : null)
        //                .takeWile(current -> !current.equals(excludeParent));
    }

    //internal
    fun toErrorLocationUrl(path: String, line: Int, column: Int): String {
        return String.format("%s://%s::%d::%d", ERROR_PROTOCOL, path, line, column)
    }

    fun fromErrorLocationUrlPath(spec: String): Pair<String, Pair<Int, Int>> {
        val parts = spec.split("::".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val file = parts[0]
        val line = if (parts.size > 1) Integer.parseInt(parts[1]) else 1
        val column = if (parts.size > 2) Integer.parseInt(parts[2]) else 1
        return Pair(file, Pair(line, column))
    }

}
