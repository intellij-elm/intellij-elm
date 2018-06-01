package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl

/**
 * A pattern which destructures a record and binds named fields.
 *
 * e.g. the 'name' and 'age' in `let {name, age} = person in ...`
 */
class ElmRecordPattern(node: ASTNode) : ElmPsiElementImpl(node) {

    val lowerPatternList: List<ElmLowerPattern>
        get() = findChildrenByClass(ElmLowerPattern::class.java).toList()
}
