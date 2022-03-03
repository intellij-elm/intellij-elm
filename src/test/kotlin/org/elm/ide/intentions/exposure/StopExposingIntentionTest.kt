package org.elm.ide.intentions.exposure

import org.elm.ide.intentions.ElmIntentionTestBase


class StopExposingIntentionTest : ElmIntentionTestBase(StopExposingIntention()) {


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


    fun `test hide a type in the exposing list`() = doAvailableTest("""
module Foo exposing (Bar, f0)
type Bar{-caret-} = BarVariant ()
f0 = ()
""", """
module Foo exposing (f0)
type Bar = BarVariant ()
f0 = ()
""")


    fun `test hide a type, which exposes its variants, in the exposing list`() = doAvailableTest("""
module Foo exposing (Bar(..), f0)
type Bar{-caret-} = BarVariant ()
f0 = ()
""", """
module Foo exposing (f0)
type Bar = BarVariant ()
f0 = ()
""")


    fun `test hide a type alias in the exposing list`() = doAvailableTest("""
module Foo exposing (Bar, f0)
type alias Bar{-caret-} = ()
f0 = ()
""", """
module Foo exposing (f0)
type alias Bar = ()
f0 = ()
""")



    // Elm does not allow an empty exposing list, so we will refuse to remove it in such cases
    // (the only alternative is to change it to `exposing (..)` or drop the module declaration
    // entirely; both are bad options).
    fun `test refuse to hide the last remaining exposed value`() = doUnavailableTest("""
module Foo exposing (bar)
bar{-caret-} = ()
""")


    fun `test refuse to hide when the module exposes everything`() = doUnavailableTest("""
module Foo exposing (..)
bar{-caret-} = ()
""")


    fun `test refuse to hide functions defined within a let-in expression`() = doUnavailableTest("""
module Foo exposing (..)
bar =
    let foo{-caret-} = ()
    in foo
""")

}
