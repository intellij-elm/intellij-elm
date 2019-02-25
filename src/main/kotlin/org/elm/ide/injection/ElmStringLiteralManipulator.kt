package org.elm.ide.injection

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.elm.lang.core.psi.elements.ElmStringConstantExpr

class ElmStringLiteralManipulator : AbstractElementManipulator<ElmStringConstantExpr>() {
    override fun handleContentChange(element: ElmStringConstantExpr, range: TextRange, newContent: String): ElmStringConstantExpr {
        if (range != getRangeInElement(element)) {
            // not supported
            return element
        }

        element.updateText(newContent)
        return element
    }

    override fun getRangeInElement(element: ElmStringConstantExpr): TextRange = element.contentOffsets
}
