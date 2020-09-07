package org.elm.ide.intentions.exposure

import org.elm.lang.core.psi.ElmExposableTag
import org.elm.lang.core.psi.elements.ElmExposingList
import org.elm.lang.core.psi.elements.ElmTypeDeclaration

/**
 * An intention action that adds a custom type to a module's `exposing` list, including its variants, i.e. it will
 * make the exposing statement something like this: `exposing (MyType(..))`.
 */
class ExposeWithVariantsIntention : ExposeIntention() {

    override fun getText() = "Expose with variants"

    override fun createContext(decl: ElmExposableTag, exposingList: ElmExposingList) =
        if (decl is ElmTypeDeclaration) Context(decl.name + "(..)", exposingList)
        else null
}
