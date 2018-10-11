package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * An underscore in a pattern.
 *
 * e.g. the `_` in `(1, _, 3)`
 */
class ElmAnythingPattern(node: ASTNode) : ElmPsiElementImpl(node), ElmConsPatternChildTag,
        ElmFunctionParamTag, ElmPatternChildTag, ElmUnionPatternChildTag
