package org.elm.ide.intentions.exposure

import org.elm.ide.intentions.ElmIntentionTestBase
import org.junit.Test

class ExposeVariantsIntentionTest : ElmIntentionTestBase(ExposeVariantsIntention()) {

    @Test
    fun `test expose variants`() = doAvailableTest(
        """
        module Foo exposing (f0, MyType)
        f0 = ()
        type MyType{-caret-} = A | B
        """.trimIndent(),

        """
        module Foo exposing (f0, MyType(..))
        f0 = ()
        type MyType = A | B
        """.trimIndent())

    @Test
    fun `test not available if variants already exposed`() = doUnavailableTest(
        """
        module Foo exposing (f0, MyType(..))
        f0 = ()
        type MyType{-caret-} = A | B
        """.trimIndent())

    @Test
    fun `test not available on type alias`() = doUnavailableTest(
        """
        module Foo exposing (f0, MyType)
        f0 = ()
        type alias MyType{-caret-} = { a: String, b: Int }
        """.trimIndent())

    @Test
    fun `test not available on function`() = doUnavailableTest(
        """
        module Foo exposing (f0, MyType)
        f0{-caret-} = ()
        type MyType = A | B
        """.trimIndent())

    @Test
    fun `test not available when module already exposes everything`() = doUnavailableTest(
        """
        module Foo exposing (..)
        f0 = ()
        type MyType{-caret-} = A | B
        """.trimIndent())
}
