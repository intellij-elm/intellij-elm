package org.elm.ide.intentions

class MakeAnnotationIntentionTest : ElmIntentionTestBase(MakeAnnotationIntention()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test value`() = doAvailableTest(
            """
module Test exposing (f)

f{-caret-} =
    1.0
"""
            , """
module Test exposing (f)

f : Float
f =
    1.0
""")

    fun `test value with docstring`() = doAvailableTest(
            """
{- docs -}
f{-caret-} =
    1.0
"""
            , """
{- docs -}
f : Float
f =
    1.0
""")

    fun `test value with caret in name`() = doAvailableTest(
            """
module Test exposing (function)

func{-caret-}tion =
    1.0
"""
            , """
module Test exposing (function)

function : Float
function =
    1.0
""")

    fun `test value with caret before name`() = doAvailableTest(
            """
module Test exposing (f)

{-caret-}f =
    1.0
"""
            , """
module Test exposing (f)

f : Float
f =
    1.0
""")


    fun `test function with unconstrained params`() = doAvailableTest(
            """
f{-caret-} a b =
    a
"""
            , """
f : a -> b -> a
f a b =
    a
""")

    fun `test function with constrained params`() = doAvailableTest(
            """
f{-caret-} a b =
    a < b
"""
            , """
f : comparable -> comparable -> Bool
f a b =
    a < b
""")

    fun `test nested value`() = doAvailableTest(
            """
f =
    let
        g{-caret-} =
            1.0
    in
        g
"""
            , """
f =
    let
        g : Float
        g =
            1.0
    in
        g
""")

    fun `test nested function`() = doAvailableTest(
            """
f =
    let
        g{-caret-} a b =
            a < b
    in
        g
"""
            , """
f =
    let
        g : comparable -> comparable -> Bool
        g a b =
            a < b
    in
        g
""")

    fun `test nested value with previous sibling`() = doAvailableTest(
            """
f =
    let
        h =
            ()
        g{-caret-} =
            1.0
    in
        g
"""
            , """
f =
    let
        h =
            ()
        g : Float
        g =
            1.0
    in
        g
""")

    fun `test nested value with caret before name`() = doAvailableTest(
            """
f =
    let
        {-caret-}function =
            1.0
    in
        function
"""
            , """
f =
    let
        function : Float
        function =
            1.0
    in
        function
""")

    fun `test nested value with caret in name`() = doAvailableTest(
            """
f =
    let
        fun{-caret-}ction =
            1.0
    in
        function
"""
            , """
f =
    let
        function : Float
        function =
            1.0
    in
        function
""")

    fun `test qualified name`() = doAvailableTestWithFileTree(
            """
--@ main.elm
import Foo as F
main{-caret-} i = F.foo i
--@ Foo.elm
module Foo exposing (..)
type alias Bar = { i : Int }
foo i = Bar i
""", """
import Foo as F
main : Int -> F.Bar
main i = F.foo i
""")
}
