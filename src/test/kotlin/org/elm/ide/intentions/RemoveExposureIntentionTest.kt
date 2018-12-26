package org.elm.ide.intentions


class RemoveExposureIntentionTest : ElmIntentionTestBase(RemoveExposureIntention()) {


    fun `test hide the first value in the exposing list`() = doAvailableTest("""
module Foo exposing (f0, f1, f2)
f0{-caret-} = ()
f1 = ()
f2 = ()
""", """
module Foo exposing (f1, f2)
f0 = ()
f1 = ()
f2 = ()
""")


    fun `test hide the middle value in the exposing list`() = doAvailableTest("""
module Foo exposing (f0, f1       ,   f2)
f0 = ()
f1{-caret-} = ()
f2 = ()
""", """
module Foo exposing (f0, f2)
f0 = ()
f1 = ()
f2 = ()
""")


    fun `test hide the last value in the exposing list`() = doAvailableTest("""
module Foo exposing (f0, f1, f2)
f0 = ()
f1 = ()
f2{-caret-} = ()
""", """
module Foo exposing (f0, f1)
f0 = ()
f1 = ()
f2 = ()
""")


    // Elm does not allow an empty exposing list, so we will refuse to remove it in such cases
    // (the only alternative is to change it to `exposing (..)` or drop the module declaration
    // entirely; both are bad options).
    fun `test refuse to hide the last remaining exposed value`() = doUnavailableTest("""
module Foo exposing (bar)
bar{-caret-} = ()
""")

}