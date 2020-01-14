package org.elm.ide.test.core

import com.intellij.openapi.util.io.FileUtil
import org.elm.ide.test.core.LabelUtils.ERROR_PROTOCOL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths

object LabelUtils {

    val DESCRIBE_PROTOCOL = "elmTestDescribe"
    val TEST_PROTOCOL = "elmTestTest"
    val ERROR_PROTOCOL = "elmTestError"

    val EMPTY_PATH = Paths.get("")

    private fun getModuleName(path: Path): String {
        return pathString(path.getName(0))
    }

    private fun encodeLabel(label: String): String {
        return URLEncoder.encode(label, "utf8")
    }

    fun decodeLabel(encoded: Path): String {
        return decodeLabel(pathString(encoded))
    }

    fun decodeLabel(encoded: String): String {
        return URLDecoder.decode(encoded, "utf8")
    }

    fun toPath(vararg labels: String): Path {
        if (labels.isEmpty()) return EMPTY_PATH
        val encoded = labels.map { encodeLabel(it) }
        return Paths.get(encoded.first(), *encoded.drop(1).toList().toTypedArray())
    }

    fun pathString(path: Path): String {
        return FileUtil.toSystemIndependentName(path.toString())
    }

    fun getName(path: Path): String {
        return decodeLabel(path.fileName)
    }

    fun toLocationUrl(path: Path, isSuite: Boolean = false): String {
        val protocol = if (isSuite) DESCRIBE_PROTOCOL else TEST_PROTOCOL
        return String.format("%s://%s", protocol, pathString(path))
    }

    /**
     * @param testsRelativeDirPath The path to the directory containing the tests, relative to the project's root (i.e.
     * the folder containing `elm.json`). The supplied `path` is the location of the test file, relative to this directory.
     */
    fun fromLocationUrlPath(path: String, testsRelativeDirPath: String): Pair<String, String> {
        val path1 = Paths.get(path)
        val moduleName = getModuleName(path1)
        val moduleFile = "$testsRelativeDirPath/${moduleName.replace(".", "/")}.elm"
        val label = if (path1.nameCount > 1) decodeLabel(path1.subpath(1, path1.nameCount)) else ""
        return Pair(moduleFile, label)
    }

    fun commonParent(path1: Path?, path2: Path): Path {
        return when {
            path1 == null -> EMPTY_PATH
            path1.nameCount > path2.nameCount -> commonParent(path2, path1)
            path2.startsWith(path1) -> path1
            else -> commonParent(path1.parent, path2)
        }
    }

    fun subParents(path: Path, excludeParent: Path): Sequence<Path> {
        if (excludeParent === EMPTY_PATH) {
            var current: Path? = path
            return generateSequence {
                current = current?.parent
                current
            }
        }

        if (!path.startsWith(excludeParent)) {
            throw IllegalStateException("not parent")
        }

        if (path === EMPTY_PATH) {
            return sequenceOf()
        }

        var current: Path? = path
        return generateSequence {
            current = current?.parent
            if (current != excludeParent) {
                current
            } else {
                null
            }
        }
    }

}

data class ErrorLabelLocation(
        val file: String,
        val line: Int,
        val column: Int
) {
    fun toUrl() =
            String.format("%s://%s::%d::%d", ERROR_PROTOCOL, file, line, column)

    companion object {
        fun fromUrl(spec: String): ErrorLabelLocation {
            val parts = spec.split("::").dropLastWhile { it.isEmpty() }
            return ErrorLabelLocation(
                    file = parts[0],
                    line = if (parts.size > 1) Integer.parseInt(parts[1]) else 1,
                    column = if (parts.size > 2) Integer.parseInt(parts[2]) else 1
            )
        }
    }
}