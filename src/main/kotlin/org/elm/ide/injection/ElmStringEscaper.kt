package org.elm.ide.injection

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.ElmStringConstantExpr

class ElmStringEscaper(
        host: ElmStringConstantExpr,
        private val isOneLine: Boolean
) : LiteralTextEscaper<ElmStringConstantExpr>(host) {
    /** A map of indexes in the decoded string to their index in the original string */
    private var outSourceOffsets: IntArray = intArrayOf()

    override fun isOneLine(): Boolean = isOneLine

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val result = outSourceOffsets.getOrNull(offsetInDecoded) ?: return -1
        val offset = if (result <= rangeInsideHost.length) result else rangeInsideHost.length
        return offset + rangeInsideHost.startOffset
    }

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val (offsets, result) = decodeEscapes(rangeInsideHost, myHost, outChars)
        outSourceOffsets = offsets
        return result
    }
}

/**
 * Based on [com.intellij.codeInsight.CodeInsightUtilCore.parseStringCharacters], but using the
 * pre-lexed elements instead of lexing the string again manually.
 */
private fun decodeEscapes(
        rangeInsideHost: TextRange,
        constantExpr: ElmStringConstantExpr,
        outChars: StringBuilder
): Pair<IntArray, Boolean> {
    val chunks = constantExpr.content
    val sourceOffsets = IntArray(rangeInsideHost.length + 1)
    val outOffset = outChars.length
    var index = 0
    for (chunk in chunks) {
        if (chunk.elementType == ElmTypes.REGULAR_STRING_PART) {
            val first = outChars.length - outOffset
            outChars.append(chunk.text)
            val last = outChars.length - outOffset - 1
            for (i in first..last) {
                sourceOffsets[i] = index
                index += 1
            }
            continue
        }

        // Set offset for the decoded character to the beginning of the escape sequence.
        sourceOffsets[outChars.length - outOffset] = index
        sourceOffsets[outChars.length - outOffset + 1] = index + 1

        when (chunk.elementType) {
            ElmTypes.STRING_ESCAPE -> {
                val text = chunk.text
                val decodeEscape = decodeEscape(text)
                outChars.append(decodeEscape)
                index += text.length
            }
            ElmTypes.INVALID_STRING_ESCAPE -> return sourceOffsets to false
            else -> error("Invalid string part $chunk")
        }
    }

    sourceOffsets[outChars.length - outOffset] = index

    return sourceOffsets to true
}

private fun decodeEscape(esc: String): String = when (esc) {
    "\\n" -> "\n"
    "\\r" -> "\r"
    "\\t" -> "\t"
    "\\\"" -> "\""
    "\\'" -> "\'"
    "\\\\" -> "\\"

    else -> {
        assert(esc.length >= 2)
        assert(esc.startsWith("\\u"))
        Integer.parseInt(esc.substring(3, esc.length - 1), 16).toChar().toString()
    }
}
