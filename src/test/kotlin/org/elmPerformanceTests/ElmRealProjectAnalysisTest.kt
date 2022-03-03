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

package org.elmPerformanceTests

import com.intellij.codeInspection.ex.InspectionToolRegistrar
import org.elm.ide.inspections.ElmLocalInspection
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.toPsiFile
import org.junit.ComparisonFailure
import java.lang.reflect.Field

/**
 * Smoke-test the plugin by evaluating it against a variety of real-world Elm projects.
 * Assuming that the projects you are testing are well-formed, these tests will detect
 * any false-positive errors caused by bugs in:
 *
 * - the parser
 * - the unresolved reference inspector
 * - the type checker
 */
class ElmRealProjectAnalysisTest : ElmRealProjectTestBase() {

    fun `test analyze elm-css`() = doTest(ELM_CSS)
    fun `test analyze elm-dev-tools`() = doTest(DEV_TOOLS)
    fun `test analyze elm-json-tree-view`() = doTest(JSON_TREE_VIEW)
    fun `test analyze elm-list-extra`() = doTest(LIST_EXTRA)
    fun `test analyze elm-physics`() = doTest(ELM_PHYSICS)
    fun `test analyze elm-spa-example`() = doTest(SPA)

    private fun doTest(info: RealProjectInfo, failOnFirstFileWithErrors: Boolean = false) {
        val inspections = InspectionToolRegistrar.getInstance().createTools()
                .map { it.tool }
                .filterIsInstance<ElmLocalInspection>()
        myFixture.enableInspections(*inspections.toTypedArray())

        println("Opening the project")
        val base = openRealProject(info) ?: return

        println("Collecting files to analyze")
        val filesToCheck = base.findDescendants {
            it.fileType == ElmFileType && run {
                val file = it.toPsiFile(project)
                file is ElmFile && file.elmProject != null
            }
        }

        if (failOnFirstFileWithErrors) {
            println("Analyzing...")
            myFixture.testHighlightingAllFiles(
                    /* checkWarnings = */ false,
                    /* checkInfos = */ false,
                    /* checkWeakWarnings = */ false,
                    *filesToCheck.toTypedArray()
            )
        } else {
            val exceptions = filesToCheck.mapNotNull { file ->
                val path = file.path.substring(base.path.length + 1)
                println("Analyzing $path")
                try {
                    myFixture.testHighlighting(
                            /* checkWarnings = */ false,
                            /* checkInfos = */ false,
                            /* checkWeakWarnings = */ false,
                            file
                    )
                    null
                } catch (e: ComparisonFailure) {
                    e to path
                }
            }

            if (exceptions.isNotEmpty()) {
                error("Error annotations found:\n\n" + exceptions.joinToString("\n\n") { (e, path) ->
                    "$path:\n${e.detailMessage}"
                })
            }
        }
    }
}

private val THROWABLE_DETAILED_MESSAGE_FIELD: Field = run {
    val field = Throwable::class.java.getDeclaredField("detailMessage")
    field.isAccessible = true
    field
}

/**
 * Retrieves original value of detailMessage field of [Throwable] class.
 * It is needed because [ComparisonFailure] overrides [Throwable.message]
 * method so we can't get the original value without reflection
 */
private val Throwable.detailMessage: CharSequence
    get() = THROWABLE_DETAILED_MESSAGE_FIELD.get(this) as CharSequence