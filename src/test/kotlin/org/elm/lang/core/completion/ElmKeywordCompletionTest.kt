package org.elm.lang.core.completion

import org.junit.Test


class ElmKeywordCompletionTest : ElmCompletionTestBase() {

    @Test
    fun `test 'type' keyword preceded by module decl`() = doSingleCompletion(
            """
module Foo exposing (..)
typ{-caret-}
""", """
module Foo exposing (..)
type {-caret-}
""")

    @Test
    fun `test 'type' keyword`() = doSingleCompletion(
            """
typ{-caret-}
""", """
type {-caret-}
""")


    @Test
    fun `test 'alias' keyword`() = doSingleCompletion(
            """
type al{-caret-}
""", """
type alias {-caret-}
""")

    @Test
    fun `test 'module' keyword`() = doSingleCompletion(
            """
mod{-caret-}
""", """
module {-caret-}
""")

    @Test
    fun `test 'module' keyword is only suggested at the beginning of a file`() = checkNoCompletion(
            """
x = 0
modu{-caret-}
""")

    @Test
    fun `test 'exposing' keyword in a module`() = doSingleCompletion(
            """
module Foo exp{-caret-}
""", """
module Foo exposing ({-caret-})
""")

    @Test
    fun `test 'exposing' keyword in an import`() = doSingleCompletion(
            """
import Foo exp{-caret-}
""", """
import Foo exposing ({-caret-})
""")

    @Test
    fun `test 'exposing' keyword on an aliased import`() = doSingleCompletion(
            """
import Foo as F exp{-caret-}
""", """
import Foo as F exposing ({-caret-})
""")

    @Test
    fun `test 'as' keyword in an import`() = doSingleCompletion(
            """
import Foo a{-caret-}
""", """
import Foo as {-caret-}
""")

    @Test
    fun `test 'if' keyword`() = doSingleCompletion(
            """
x =
    if{-caret-}
""", """
x =
    if {-caret-}
""")

    @Test
    fun `test 'then' keyword`() = doSingleCompletion(
            """
x =
    if True th{-caret-}
""", """
x =
    if True then{-caret-}
""")

    @Test
    fun `test 'else' keyword`() = doSingleCompletion(
            """
x =
    if True then 1 el{-caret-}
""", """
x =
    if True then 1 else {-caret-}
""")

    @Test
    fun `test 'case' keyword`() = doSingleCompletion(
            """
x =
    ca{-caret-}
""", """
x =
    case {-caret-}
""")

    @Test
    fun `test 'of' keyword`() = doSingleCompletion(
            """
f x =
    case x o{-caret-}
""", """
f x =
    case x of{-caret-}
""")

    @Test
    fun `test 'let' keyword`() = doSingleCompletion(
            """
x = le{-caret-}
""", """
x = let{-caret-}
""")

    @Test
    fun `test 'in' keyword on separate line normal`() = doSingleCompletion(
            """
x = let
        a = 42
    in{-caret-}
""", """
x = let
        a = 42
    in{-caret-}
""")

    @Test
    fun `test 'in' keyword on separate line indented`() = doSingleCompletion(
            """
x = let
        a = 42
        in{-caret-}
""", """
x = let
        a = 42
        in{-caret-}
""")


    // keywords like 'if', 'case' and 'let' should be suggested in any context that begins a new expression


    @Test
    fun `test keywords that can begin an expression after a left paren`() = doSingleCompletion(
            """
x =
    (i{-caret-}, 0)
""", """
x =
    (if {-caret-}, 0)
""")

    @Test
    fun `test keywords that can begin an expression after a left square bracket`() = doSingleCompletion(
            """
x =
    [i{-caret-}, 0]
""", """
x =
    [if {-caret-}, 0]
""")

    @Test
    fun `test keywords that can begin an expression after 'in'`() = doSingleCompletion(
            """
x =
    let y = 0
    in
    i{-caret-}
""", """
x =
    let y = 0
    in
    if {-caret-}
""")

    @Test
    fun `test keywords that can begin an expression after case branch arrow`() = doSingleCompletion(
            """
x = case () of
        () -> i{-caret-}
""", """
x = case () of
        () -> if {-caret-}
""")

}