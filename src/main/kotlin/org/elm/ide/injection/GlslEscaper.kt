package org.elm.ide.injection

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import org.elm.lang.core.psi.elements.ElmGlslCodeExpr

// GLSL blocks don't require any escaping, so this is a passthrough
class GlslEscaper(host: ElmGlslCodeExpr) : LiteralTextEscaper<ElmGlslCodeExpr>(host) {
    override fun isOneLine(): Boolean = false

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        return rangeInsideHost.startOffset + offsetInDecoded
    }

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        outChars.append(rangeInsideHost.substring(myHost.text))
        return true
    }
}
