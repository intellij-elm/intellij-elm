package org.elm.lang.core.psi

import com.intellij.psi.tree.IElementType
import org.elm.lang.core.ElmLanguage

class ElmTokenType(debugName: String
) : IElementType(debugName, ElmLanguage) {

    override fun toString(): String {
        return "ElmTokenType.${super.toString()}"
    }
}

class ElmElementType(debugName: String
) : IElementType(debugName, ElmLanguage)
