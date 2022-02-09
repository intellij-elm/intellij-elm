package org.elm.lang.core.completion

import org.junit.Test

class ElmCompletionTest : ElmCompletionTestBase() {


    @Test
    fun `test value completion from function parameter`() = doSingleCompletion(
            """
view model = text mo{-caret-}
""", """
view model = text model{-caret-}
""")


    fun `test value completion from let-in decl`() = doSingleCompletion(
            """
f = let name = "Arnold" in na{-caret-}
""", """
f = let name = "Arnold" in name{-caret-}
""")


    fun `test value completion from case-of pattern destructuring`() = doSingleCompletion(
            """
f = case user of { name, age } -> nam{-caret-}
""", """
f = case user of { name, age } -> name{-caret-}
""")


    fun `test union constructor completion from pattern destructuring`() = doSingleCompletion(
            """
type MyState = State Int
f (Sta{-caret-} n) = n
""", """
type MyState = State Int
f (State{-caret-} n) = n
""")


    fun `test union type completion in a type annotation`() = doSingleCompletion(
            """
type Page = Home
defaultPage : Pa{-caret-}
""", """
type Page = Home
defaultPage : Page{-caret-}
""")


    fun `test type alias completion in a type annotation`() = doSingleCompletion(
            """
type alias User = { name : String, age : Int }
viewUser : Us{-caret-}
""", """
type alias User = { name : String, age : Int }
viewUser : User{-caret-}
""")


    fun `test qualified value completion`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import User
g = User.defa{-caret-}

--@ User.elm
module User exposing (..)
defaultUser = "Arnold"
""", """
import User
g = User.defaultUser{-caret-}

""")


    fun `test qualified value completion also includes non-imported modules`() = doSingleCompletionMultiFile(
            """
--@ main.elm
g = User.defa{-caret-}

--@ User.elm
module User exposing (..)
defaultUser = "Arnold"
""", """
g = User.defaultUser{-caret-}

""")


    fun `test qualified type completion`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import User
g : User.Us{-caret-}

--@ User.elm
module User exposing (..)
type User = String
""", """
import User
g : User.User{-caret-}

""")


    fun `test qualified type completion also includes non-imported modules`() = doSingleCompletionMultiFile(
            """
--@ main.elm
g : User.Us{-caret-}

--@ User.elm
module User exposing (..)
type User = String
""", """
g : User.User{-caret-}

""")


    fun `test qualified union constructor completion in expr`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Page
defaultPage = Page.Ho{-caret-}

--@ Page.elm
module Page exposing (..)
type Page = Home
""", """
import Page
defaultPage = Page.Home{-caret-}

""")


    fun `test qualified union constructor completion in pattern`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Page
defaultPage p = case p of
    Page.Ho{-caret-}

--@ Page.elm
module Page exposing (..)
type Page = Home
""", """
import Page
defaultPage p = case p of
    Page.Home{-caret-}

""")


    fun `test does not complete union constructors in type namespace`() = checkNoCompletion(
            """
type Page = NotFound
f : NotF{-caret-}
""")


    fun `test does not complete number literals`() = checkNoCompletion(
            """
x = 42
y = 4{-caret-}
""")


// TODO [kl] eventually code completion should add a 'dot' suffix when completing a module qualifier


    fun `test module completion of module prefix, after dot`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Data.User
g = Data.{-caret-}

--@ Data/User.elm
module Data.User exposing (..)
""", """
import Data.User
g = Data.User{-caret-}

""")


    fun `test module name completion with caret before dot`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Data.User
g = Dat{-caret-}

--@ Data/User.elm
module Data.User exposing (..)
""", """
import Data.User
g = Data{-caret-}

""")


    fun `test module name completion with caret after dot`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Data.User
g = Data.{-caret-}

--@ Data/User.elm
module Data.User exposing (..)
""", """
import Data.User
g = Data.User{-caret-}

""")


    fun `test qualified value completion with caret after dot`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import User
g = User.{-caret-}

--@ User.elm
module User exposing (..)
defaultUser = "Arnold"
""", """
import User
g = User.defaultUser{-caret-}

""")


    /*
    TODO investigate

    This test is temporarily disabled until we can figure out how to conditionally change
    the IntelliJ dummy identifier to use an upper-case identifier in contexts such as types
    which must start with an upper-case letter.

    fun `test qualified type completion with caret after dot`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Foo
g : Foo.{-caret-}

--@ Foo.elm
module Foo exposing (Bar)
type Bar = Baz String
""", """
import Foo
g : Foo.Bar{-caret-}

""")
*/


    fun `test qualified type constructor completion with caret after dot`() = doSingleCompletionMultiFile(
            """
--@ main.elm
import Foo
g = Foo.{-caret-}

--@ Foo.elm
module Foo exposing (..)
type Bar = Baz String
""", """
import Foo
g = Foo.Baz{-caret-}

""")

}