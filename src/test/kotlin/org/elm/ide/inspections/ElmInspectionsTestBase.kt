package org.elm.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language


abstract class ElmAnnotationTestBase : ElmTestBase() {
    protected fun doTest(vararg additionalFilenames: String) {
        myFixture.testHighlighting(fileName, *additionalFilenames)
    }

    protected fun checkByText(
            @Language("Elm") text: String,
            checkWarn: Boolean = true,
            checkInfo: Boolean = false,
            checkWeakWarn: Boolean = false
    ) = check(text,
            checkWarn = checkWarn,
            checkInfo = checkInfo,
            checkWeakWarn = checkWeakWarn,
            configure = this::configureByText)

    protected fun checkByFileTree(
            @Language("Elm") text: String,
            checkWarn: Boolean = true,
            checkInfo: Boolean = false,
            checkWeakWarn: Boolean = false
    ) = check(text,
            checkWarn = checkWarn,
            checkInfo = checkInfo,
            checkWeakWarn = checkWeakWarn,
            configure = this::configureByFileTree)

    private fun checkByText(text: String) {
        myFixture.checkResult(replaceCaretMarker(text.trimIndent()))
    }

    private fun check(
            @Language("Elm") text: String,
            checkWarn: Boolean,
            checkInfo: Boolean,
            checkWeakWarn: Boolean,
            configure: (String) -> Unit
    ) {
        configure(text)
        myFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn)
    }
}

abstract class ElmInspectionsTestBase(
        val inspection: LocalInspectionTool
) : ElmAnnotationTestBase() {

    private fun enableInspection() = myFixture.enableInspections(inspection)

    override fun configureByText(text: String) {
        super.configureByText(text)
        enableInspection()
    }

    override fun configureByFileTree(text: String) {
        super.configureByFileTree(text)
        enableInspection()
    }
}
