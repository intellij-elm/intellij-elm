package org.elm.lang

import org.elm.lang.core.ElmFileType
import java.nio.file.Paths


interface ElmTestCase {

    companion object {
        val testResourcesPath = "src/test/resources"
    }

    /**
     * The relative path to the test fixture data within the Test Resources root
     * (e.g. "org.elm.lang.core.lexer"
     */
    fun getTestDataPath(): String
}

/**
 * Path to the source text file which is the input to the test.
 */
fun ElmTestCase.pathToSourceTestFile(name: String) =
        Paths.get("${ElmTestCase.testResourcesPath}/${getTestDataPath()}/$name.${ElmFileType.EXTENSION}")

/**
 * Path to the 'gold' reference file which is the expected output of the test.
 */
fun ElmTestCase.pathToGoldTestFile(name: String) =
        Paths.get("${ElmTestCase.testResourcesPath}/${getTestDataPath()}/$name.txt")

