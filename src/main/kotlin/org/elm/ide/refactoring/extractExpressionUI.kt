package org.elm.ide.refactoring

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Pass
import com.intellij.refactoring.IntroduceTargetChooser
import org.elm.lang.core.psi.ElmExpressionTag
import org.elm.openapiext.isUnitTestMode
import org.jetbrains.annotations.TestOnly

fun showExpressionChooser(
        editor: Editor,
        exprs: List<ElmExpressionTag>,
        callback: (ElmExpressionTag) -> Unit
) {
    if (isUnitTestMode) {
        callback(MOCK!!.chooseTarget(exprs))
    } else {
        IntroduceTargetChooser.showChooser(editor, exprs, callback.asPass) { it.text }
    }
}

private val <T> ((T) -> Unit).asPass: Pass<T>
    get() = object : Pass<T>() {
        override fun pass(t: T) = this@asPass(t)
    }

interface ExtractExpressionUi {
    fun chooseTarget(exprs: List<ElmExpressionTag>): ElmExpressionTag
}

private var MOCK: ExtractExpressionUi? = null
@TestOnly
fun withMockTargetExpressionChooser(mock: ExtractExpressionUi, f: () -> Unit) {
    MOCK = mock
    try {
        f()
    } finally {
        MOCK = null
    }
}
