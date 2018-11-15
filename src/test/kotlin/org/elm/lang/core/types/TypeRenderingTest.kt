package org.elm.lang.core.types

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeRenderingTest {

    @Test
    fun `nested list`() = doTest(
            TyList(TyList(TyVar("a"))),
            "List (List a)"
    )

    @Test
    fun `linkified type`() = doTest(
            TyUnion("Module.Submod", "Type", parameters = emptyList()),
            "List (List a)",
            linkify = true
    )

    private fun doTest(ty: Ty, expected: String, linkify: Boolean = false, withModule: Boolean = false) {
        assertEquals(expected, ty.renderedText(linkify, withModule))
    }
}
