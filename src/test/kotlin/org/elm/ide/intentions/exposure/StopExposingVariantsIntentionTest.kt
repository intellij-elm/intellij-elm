package org.elm.ide.intentions.exposure

import org.elm.ide.intentions.ElmIntentionTestBase
import org.junit.Test

class StopExposingVariantsIntentionTest : ElmIntentionTestBase(StopExposingVariantsIntention()) {

    @Test
    fun `test stop exposing variants`() = doAvailableTest(
        """
        module Foo exposing (f0, MyType(..))
        f0 = ()
        type MyType{-caret-} = A | B
        """.trimIndent(),

        """
        module Foo exposing (f0, MyType)
        f0 = ()
        type MyType = A | B
        """.trimIndent())

    @Test
    fun `test not available if variants not currently exposed`() = doUnavailableTest(
        """
        module Foo exposing (f0, MyType)
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
    fun `test not available when module exposes everything`() = doUnavailableTest(
        """
        module Foo exposing (..)
        f0 = ()
        type MyType{-caret-} = A | B
        """.trimIndent())
}
