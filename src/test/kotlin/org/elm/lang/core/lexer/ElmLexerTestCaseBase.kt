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

package org.elm.lang.core.lexer

import com.intellij.lexer.Lexer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.UsefulTestCase
import org.elm.lang.ElmTestCase
import org.elm.lang.pathToGoldTestFile
import org.elm.lang.pathToSourceTestFile
import org.jetbrains.annotations.NonNls
import java.io.IOException


abstract class ElmLexerTestCaseBase : LexerTestCase(), ElmTestCase {
    override fun getDirPath(): String = throw UnsupportedOperationException()

    // NOTE(matkad): this is basically a copy-paste of doFileTest.
    // The only difference is that encoding is set to utf-8
    protected fun doTest(lexer: Lexer = createLexer()) {
        val filePath = pathToSourceTestFile(getTestName(false))
        var text = ""
        try {
            val fileText = FileUtil.loadFile(filePath.toFile(), CharsetToolkit.UTF8)
            text = StringUtil.convertLineSeparators(if (shouldTrim()) fileText.trim() else fileText)
        } catch (e: IOException) {
            fail("can't load file " + filePath + ": " + e.message)
        }
        doTest(text, null, lexer)
    }

    override fun doTest(@NonNls text: String, expected: String?, lexer: Lexer) {
        val result = printTokens(text, 0, lexer)
        if (expected != null) {
            UsefulTestCase.assertSameLines(expected, result)
        } else {
            UsefulTestCase.assertSameLinesWithFile(pathToGoldTestFile(getTestName(false)).toFile().canonicalPath, result)
        }
    }
}
