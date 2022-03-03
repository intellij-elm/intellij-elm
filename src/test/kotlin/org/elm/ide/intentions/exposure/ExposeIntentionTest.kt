package org.elm.ide.intentions.exposure

import org.elm.ide.intentions.ElmIntentionTestBase


class ExposeIntentionTest : ElmIntentionTestBase(ExposeIntention()) {


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


    fun `test cannot expose when the module already exposes everything`() = doUnavailableTest(
            """
module Foo exposing (..)
f0 = ()
f1{-caret-} = ()
""")


    fun `test cannot expose functions defined in a let-in expression`() = doUnavailableTest(
            """
module Foo exposing (f0)
f0 =
    let f1{-caret-} = ()
    in f1
""")


}
