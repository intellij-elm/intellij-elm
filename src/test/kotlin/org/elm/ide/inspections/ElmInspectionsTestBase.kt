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

    protected fun checkFixByText(
            fixName: String,
            @Language("Elm") before: String,
            @Language("Elm") after: String,
            checkWarn: Boolean = true,
            checkInfo: Boolean = false,
            checkWeakWarn: Boolean = false
    ) = checkFix(fixName, before, after,
            configure = this::configureByText,
            checkBefore = { myFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn) },
            checkAfter = this::checkByText)

    protected fun checkFixByTextWithoutHighlighting(
            fixName: String,
            @Language("Elm") before: String,
            @Language("Elm") after: String
    ) = checkFix(fixName, before, after,
            configure = this::configureByText,
            checkBefore = {},
            checkAfter = this::checkByText)

    private fun checkFix(
            fixName: String,
            @Language("Elm") before: String,
            @Language("Elm") after: String,
            configure: (String) -> Unit,
            checkBefore: () -> Unit,
            checkAfter: (String) -> Unit
    ) {
        configure(before)
        checkBefore()
        applyQuickFix(fixName)
        checkAfter(after)
    }

    protected fun checkFixByFileTree(
            fixName: String,
            @Language("Elm") treeText: String,
            @Language("Elm") after: String,
            checkWarn: Boolean = true,
            checkInfo: Boolean = false,
            checkWeakWarn: Boolean = false
    ) = checkFix(fixName, treeText, after,
            configure = this::configureByFileTree,
            checkBefore = { myFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn) },
            checkAfter = this::checkByText)

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
