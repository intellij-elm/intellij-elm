package org.elm.ide.intentions.exposure

import org.elm.ide.intentions.ElmIntentionTestBase
import org.junit.Test


class ExposeIntentionTest : ElmIntentionTestBase(ExposeIntention()) {


    @Test
    fun `test expose a function`() = doAvailableTest(
            """
module Foo exposing (f0)
f0 = ()
f1{-caret-} = ()
""", """
module Foo exposing (f0, f1)
f0 = ()
f1 = ()
""")


    @Test
    fun `test cannot expose when the module already exposes everything`() = doUnavailableTest(
            """
module Foo exposing (..)
f0 = ()
f1{-caret-} = ()
""")


    @Test
    fun `test cannot expose functions defined in a let-in expression`() = doUnavailableTest(
            """
module Foo exposing (f0)
f0 =
    let f1{-caret-} = ()
    in f1
""")


}
