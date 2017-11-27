package org.elm.lang.core.resolve

class ElmAliasedImportResolveTest : ElmResolveTestBase() {


    fun `test aliased, qualified value ref`() = stubOnlyResolve(
"""
--@ main.elm
import Foo as F
main = F.bar
         --^Foo.elm

--@ Foo.elm
module Foo exposing (bar)
bar = 42
""")


    fun `test aliased, qualified union type ref`() = stubOnlyResolve(
"""
--@ main.elm
import App as A
type alias Model = A.Page
                     --^App.elm

--@ App.elm
module App exposing (Page)
type Page = Home
""")


    fun `test aliased, qualified union constructor ref`() = stubOnlyResolve(
"""
--@ main.elm
import App as A
defaultPage = A.Home
                --^App.elm

--@ App.elm
module App exposing (Page(Home))
type Page = Home
""")


    fun `test aliased, qualified type alias ref`() = stubOnlyResolve(
"""
--@ main.elm
import App as A
type Entity = PersonEntity A.Person
                             --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")


    fun `test aliased, qualified record constructor ref`() = stubOnlyResolve(
"""
--@ main.elm
import App as A
defaultPerson = A.Person "George" 42
                  --^App.elm

--@ App.elm
module App exposing (Person)
type alias Person = { name : String, age: Int }
""")

}