/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.lang

import org.elm.lang.core.ElmFileType
import java.nio.file.Paths


interface ElmTestCase {

    companion object {
        val testResourcesPath = "src/test/resources"
    }

    /**
     * The relative path to the test fixture data within the Test Resources root.
     *
     * This is the key data that [LightPlatformCodeInsightFixtureTestCase] needs
     * to be able to find the location of your test fixtures, and it *MUST* be
     * overridden by IntelliJ plugins. Unfortunately, the method is not abstract
     * so we can't rely on the compiler to verify that we have overridden it, so
     * we instead add it to this interface, thereby forcing the client to provide
     * an override.
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
