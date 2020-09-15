package org.elm.ide.intentions

class RecordConstructorToLiteralIntentionTest : ElmIntentionTestBase(RecordConstructorToLiteralIntention()) {

    fun `test record type alias with two fields (caret inside type name)`() = doAvailableTest(
        """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

foo =
    MyType{-caret-}Alias "A" 1
""", """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

foo =
    { a = "A", b = 1 }
"""
    )

    fun `test record type alias with two fields (caret at end of type name)`() = doAvailableTest(
        """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

foo =
    MyTypeAlias{-caret-} "A" 1
""", """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

foo =
    { a = "A", b = 1 }
"""
    )

    fun `test record type alias with nested record`() = doAvailableTest(
        """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int, c : { x : String, y : String} }

foo =
    MyType{-caret-}Alias "A" 1 { x = "X", y = "Y" }
""", """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int, c : { x : String, y : String} }

foo =
    { a = "A", b = 1, c = { x = "X", y = "Y" } }
"""
    )


    fun `test record type alias with nested record type alias (using constructor function)`() = doAvailableTest(
        """
module Foo exposing (..)

type alias MyNestedTypeAlias =
    { x : String, y : String}

type alias MyTypeAlias =
    { a : String, b : Int, c : MyNestedTypeAlias }

foo =
    MyType{-caret-}Alias "A" 1 (MyNestedTypeAlias "X" "Y")
""", """
module Foo exposing (..)

type alias MyNestedTypeAlias =
    { x : String, y : String}

type alias MyTypeAlias =
    { a : String, b : Int, c : MyNestedTypeAlias }

foo =
    { a = "A", b = 1, c = (MyNestedTypeAlias "X" "Y") }
"""
    )

    fun `test record type alias with nested record type alias (using record literal)`() = doAvailableTest(
        """
module Foo exposing (..)

type alias MyNestedTypeAlias =
    { x : String, y : String}

type alias MyTypeAlias =
    { a : String, b : Int, c : MyNestedTypeAlias }

foo =
    MyType{-caret-}Alias "A" 1 { x = "X", y = "Y", c = { x = "X", y = "Y" } }
""", """
module Foo exposing (..)

type alias MyNestedTypeAlias =
    { x : String, y : String}

type alias MyTypeAlias =
    { a : String, b : Int, c : MyNestedTypeAlias }

foo =
    { a = "A", b = 1, c = { x = "X", y = "Y", c = { x = "X", y = "Y" } } }
"""
    )

    fun `test record type alias with non-trivial values`() = doAvailableTest(
        """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

foo =
    MyType{-caret-}Alias ("A" ++ "B" ++ missingFunction) (1 * 2 * (String.length "bar"))
""", """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

foo =
    { a = ("A" ++ "B" ++ missingFunction), b = (1 * 2 * (String.length "bar")) }
"""
    )

    fun `test record type alias imported from another module (unqualified)`() = doAvailableTestWithFileTree(
        """
--@ main.elm
module Main exposing (..)

import Foo exposing (..)

foo =
    MyType{-caret-}Alias "A" 1
--@ Foo.elm
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }
""", """
module Main exposing (..)

import Foo exposing (..)

foo =
    { a = "A", b = 1 }
"""
    )

    fun `test record type alias imported from another module (qualified)`() = doAvailableTestWithFileTree(
        """
--@ main.elm
module Main exposing (..)

import Foo

foo =
    Foo.MyType{-caret-}Alias "A" 1
--@ Foo.elm
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }
""", """
module Main exposing (..)

import Foo

foo =
    { a = "A", b = 1 }
"""
    )

    fun `test not available for custom type`() = doUnavailableTest(
        """
module Foo exposing (..)

type MyType
    = MyType String Int

foo =
    My{-caret-}Type "a" 1
"""
    )

    fun `test not available for function which creates record`() = doUnavailableTest(
        """
module Foo exposing (..)

type alias MyTypeAlias =
    { a : String, b : Int }

constructMyTypeAlias : String -> Int -> MyTypeAlias
constructMyTypeAlias string int =
    MyTypeAlias string int

foo =
    constructMyType{-caret-}Alias "A" 1
"""
    )
}
