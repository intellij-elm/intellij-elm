package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionParamTag
import org.elm.lang.core.psi.ElmPatternChildTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmUnionPatternChildTag


/**
 * An underscore in a pattern.
 *
 * e.g. the `_` in `(1, _, 3)`
 */
class ElmAnythingPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag
