package org.elm.lang.core.resolve

import org.junit.Test


/**
 * Tests related to resolving references via an import declaration, crossing file boundaries.
 */
class ElmImportResolveTest: ElmResolveTestBase() {


    @Test
    fun `test value ref in import declaration`() = stubOnlyResolve(
"""
--@ main.elm
import Foo exposing (bar)
                     --^Foo.elm
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    @Test
    fun `test value ref from expression`() = stubOnlyResolve(
"""
--@ main.elm
import Foo exposing (bar)
main = bar
       --^Foo.elm
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    @Test
    fun `test value ref from expression but not exposed by import`() = stubOnlyResolve(
"""
--@ main.elm
import Foo
main = bar
       --^unresolved
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    @Test
    fun `test value ref from expression but not exposed by module`() = stubOnlyResolve(
"""
--@ main.elm
import Foo exposing (bar)
main = bar
       --^unresolved
--@ Foo.elm
module Foo exposing ()
bar = 42
""")


    @Test
    fun `test import of hierarchical module`() = stubOnlyResolve(
"""
--@ main.elm
import Foo.Bar exposing (bar)
                         --^Foo/Bar.elm
--@ Foo/Bar.elm
module Foo.Bar exposing (bar)
bar = 42
""")


    @Test
    fun `test value import exposing all`() = stubOnlyResolve(
"""
--@ main.elm
import Foo exposing (..)
f = bar
   --^Foo.elm

--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")

    @Test
    fun `test value import exposing all from both sides`() = stubOnlyResolve(
"""
--@ main.elm
import Foo exposing (..)
f = bar
   --^Foo.elm

--@ Foo.elm
module Foo exposing (..)
bar = 42
""")

    @Test
    fun `test value import exposing all but not exposed by module`() = stubOnlyResolve(
"""
--@ main.elm
import Foo exposing (..)
f = bar
   --^unresolved

--@ Foo.elm
module Foo exposing ()
bar = 42
""")



    @Test
    fun `test union type ref in import declaration`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page)
                     --^App.elm

--@ App.elm
module App exposing (Page)
type Page = Home
""")


    @Test
    fun `test union type ref in type definition`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page)
type alias Model = Page
                   --^App.elm

--@ App.elm
module App exposing (Page)
type Page = Home
""")


    @Test
    fun `test union type ref in body but not exposed by import`() = stubOnlyResolve(
"""
--@ main.elm
import App
type alias Model = Page
                   --^unresolved

--@ App.elm
module App exposing (Page)
type Page = Home
""")


    @Test
    fun `test union type ref in body but not exposed by module`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page)
type alias Model = Page
                   --^unresolved

--@ App.elm
module App exposing ()
type Page = Home
""")


    @Test
    fun `test union constructor ref in expression`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page(..))
defaultPage = Home
              --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home
""")


    @Test
    fun `test union constructor ref in expression via import exposing all constructors`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page(..))
defaultPage = Home
              --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home
""")


    @Test
    fun `test union constructor ref in expression via module exposing all constructors`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page(..))
defaultPage = Home
              --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home
""")

    @Test
    fun `test union constructor ref in expression exposing all from both sides`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (..)
defaultPage = Home
              --^App.elm

--@ App.elm
module App exposing (..)
type Page = Home
""")


    @Test
    fun `test union constructor ref in expression but not exposed by module`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page(Home))
defaultPage = Home
              --^unresolved

--@ App.elm
module App exposing (Page)
type Page = Home
""")

    @Test
    fun `test union constructor ref preceeded by incomplete import`() = stubOnlyResolve(
            """
--@ main.elm
import Foo as
import App exposing (Page(..))
defaultPage = Home
              --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home

--@Foo.elm
module Foo exposing(..)
""")



    @Test
    fun `test union constructor ref in pattern destructuring`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Page(..))
title page =
    case page of
        Home -> "home"
        --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home
""")



    @Test
    fun `test type alias ref in import declaration`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Person)
                     --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")


    @Test
    fun `test type alias ref in body`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Person)
type Entity = PersonEntity Person
                           --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")

    @Test
    fun `test type alias import exposing all from both sides`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (..)
type Entity = PersonEntity Person
                           --^App.elm

--@ App.elm
module App exposing (..)
type alias Person = { name : String, age: Int }
""")



    @Test
    fun `test record constructor ref in expression`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (Person)
defaultPerson = Person "George" 42
                --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")

    @Test
    fun `test record constructor import exposing all from both sides`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (..)
defaultPerson = Person "George" 42
                --^App.elm

--@ App.elm
module App exposing (..)
type alias Person = { name : String, age: Int }
""")




    @Test
    fun `test union type import exposing all`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (..)
type alias Model = Page
                   --^App.elm

--@ App.elm
module App exposing (Page)
type Page = Home
""")

    @Test
    fun `test union type import exposing all from both sides`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (..)
type alias Model = Page
                   --^App.elm

--@ App.elm
module App exposing (..)
type Page = Home
""")


    @Test
    fun `test union type import exposing all but not exposed by module`() = stubOnlyResolve(
"""
--@ main.elm
import App exposing (..)
type alias Model = Page
                   --^unresolved

--@ App.elm
module App exposing ()
type Page = Home
""")


    @Test
    fun `test module-name ref from import`() = stubOnlyResolve(
"""
--@ main.elm
import App
       --^App.elm

--@ App.elm
module App exposing (..)
""")

    @Test
    fun `test dotted module-name ref from import`() = stubOnlyResolve(
"""
--@ main.elm
import Data.User
       --^Data/User.elm

--@ Data/User.elm
module Data.User exposing (..)
""")


    @Test
    fun `test port ref`() = stubOnlyResolve(
"""
--@ main.elm
import Ports exposing (foo)
                       --^Ports.elm

--@ Ports.elm
port module Ports exposing (foo)
port foo : String -> Cmd msg
""")


    // BINARY OPERATORS

    @Test
    fun `test binary operator in import exposing list`() = stubOnlyResolve(
"""
--@ main.elm
import Math exposing ((**))
                      --^Math.elm

--@ Math.elm
module Math exposing ((**))
infix left 5 (**) = power
power a b = 42
""")


    @Test
    fun `test binary operator usage in value expression`() = stubOnlyResolve(
"""
--@ main.elm
import Math exposing ((**))
f = 2 ** 3
     --^Math.elm

--@ Math.elm
module Math exposing ((**))
infix left 5 (**) = power
power a b = 42
""")


    @Test
    fun `test binary operator via import exposing all`() = stubOnlyResolve(
"""
--@ main.elm
import Math exposing (..)
f = 2 ** 3
     --^Math.elm

--@ Math.elm
module Math exposing ((**))
infix left 5 (**) = power
power a b = 42
""")


    @Test
    fun `test binary operator as a function`() = stubOnlyResolve(
"""
--@ main.elm
import Math exposing ((**))
f = (**) 2 3
    --^Math.elm

--@ Math.elm
module Math exposing ((**))
infix left 5 (**) = power
power a b = 42
""")

}
