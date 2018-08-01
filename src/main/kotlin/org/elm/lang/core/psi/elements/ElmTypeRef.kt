package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.directChildren


/**
 * A type reference.
 *
 * e.g.
 *
 *  - `Float`
 *  - `Maybe a`
 *  - `Int -> String`
 *  - `a -> (a -> {a: String})`
 */
class ElmTypeRef(node: ASTNode) : ElmPsiElementImpl(node) {

    /**
     * All parameters of the type annotation.
     *
     * The elements will be in source order, and will be any of the following types:
     *
     * [ElmTypeVariableRef], [ElmRecordType], [ElmTupleType], [ElmParametricTypeRef], [ElmTypeRef]
     *
     * If the reference is not a function, there will be one parameter in well-formed programs. For functions, there
     * will be one parameter per function argument, plus the return type.
     */
    val allParameters: Sequence<ElmPsiElement>
        get() = directChildren.filterIsInstance<ElmPsiElement>().filter {
            it is ElmUpperPathTypeRef
                    || it is ElmTypeVariableRef
                    || it is ElmRecordType
                    || it is ElmTupleType
                    || it is ElmParametricTypeRef
                    || it is ElmTypeRef
        }
}
