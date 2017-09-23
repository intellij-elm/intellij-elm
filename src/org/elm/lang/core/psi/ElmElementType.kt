package org.elm.lang.core.psi

import com.intellij.psi.tree.IElementType
import org.elm.lang.core.ElmLanguage

/** type of intermediate PSI tree nodes */
class ElmElementType(debugName: String) : IElementType(debugName, ElmLanguage)
