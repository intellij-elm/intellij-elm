package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypes


/**
 * Describes the union constructors exposed from a union type.
 *
 * There are 2 cases:
 *
 * 1. [doubleDot] is null and [exposedUnionConstructors] contains a list of explicitly
 *    exposed constructors (e.g. `import App exposing Page(Home, NotFound)`)
 * 2. [doubleDot] is not-null, in which case all constructors are exposed
 *    (e.g. `import App exposing Page(..)`)
 */
class ElmExposedUnionConstructors(node: ASTNode) : ElmPsiElementImpl(node) {

    val doubleDot: PsiElement?
        get() = findChildByType(ElmTypes.DOUBLE_DOT)


    val exposedUnionConstructors: List<ElmExposedUnionConstructor>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmExposedUnionConstructor::class.java)

}
