package org.elm.lang.core.parser.manual


import com.intellij.lang.PsiBuilder
import java.util.regex.Pattern


internal object IndentationHelper {
    private val indentationPattern = Pattern.compile(".*[\r\n]([^\r\n]+)$", Pattern.DOTALL)

    fun getIndentationOfPreviousToken(builder: PsiBuilder): Int {
        // getTokenType has some side effects. Do not remove the call.
        builder.getTokenType()
        val end = builder.rawTokenTypeStart(0)
        return getIndentation(builder.originalText, 0, end)
    }

    fun getIndentation(text: CharSequence, start: Int, end: Int): Int {
        val previousChars = text.subSequence(start, end)
        val m = indentationPattern.matcher(previousChars)
        return if (m.matches()) m.group(1).length else 0
    }
}
