package org.elm.lang.core.resolve

class ElmQualifiedImportResolveTest: ElmResolveTestBase() {


    fun `test qualified value ref`() = stubOnlyResolve(
"""
--@ main.elm
import Foo
main = Foo.bar
           --^Foo.elm
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    fun `test qualified value ref with caret on the qualifier`() = stubOnlyResolve(
            """
--@ main.elm
import Foo
main = Foo.bar
      --^Foo.elm
--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    fun `test qualified union type ref`() = stubOnlyResolve(
"""
--@ main.elm
import App
type alias Model = App.Page
                       --^App.elm

--@ App.elm
module App exposing (Page)
type Page = Home
""")


    fun `test qualified union constructor ref`() = stubOnlyResolve(
"""
--@ main.elm
import App
defaultPage = App.Home
                  --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home
""")


    fun `test qualified union constructor ref in pattern destructuring`() = stubOnlyResolve(
"""
--@ main.elm
import App
title page =
    case page of
        App.Home -> "home"
            --^App.elm

--@ App.elm
module App exposing (Page(..))
type Page = Home
""")



    fun `test qualified type alias ref`() = stubOnlyResolve(
"""
--@ main.elm
import App
type Entity = PersonEntity App.Person
                               --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")


    fun `test qualified record constructor ref`() = stubOnlyResolve(
"""
--@ main.elm
import App
defaultPerson = App.Person "George" 42
                    --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")


    fun `test qualified port ref`() = stubOnlyResolve(
"""
--@ main.elm
import Ports
update msg model = (model, Ports.foo "blah")
                                 --^Ports.elm

--@ Ports.elm
port module Ports exposing (foo)
port foo : String -> Cmd msg
""")


}