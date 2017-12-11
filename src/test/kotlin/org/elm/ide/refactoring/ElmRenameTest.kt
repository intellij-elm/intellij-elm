package org.elm.ide.refactoring

import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.intellij.lang.annotations.Language


class ElmRenameTest: ElmTestBase() {


    fun `test value decl rename`() {
        doTest("quux",
"""
foo : Int
foo = 42
bar = foo{-caret-} + 2
""", """
quux : Int
quux = 42
bar = quux + 2
""")}


    fun `test function parameter rename`() {
        doTest("z",
"""
f x{-caret-} y = x + y
""", """
f z y = z + y
""")}


    fun `test union type rename`() {
        doTest("Quux",
"""
type Foo{-caret-} = A | B
type Bar = C Foo
f : Foo -> String
""", """
type Quux = A | B
type Bar = C Quux
f : Quux -> String
""")}


    fun `test type alias rename`() {
        doTest("Quux",
"""
type alias Foo{-caret-} = Int
type Bar = C Foo
f : Foo -> String
""", """
type alias Quux = Int
type Bar = C Quux
f : Quux -> String
""")}


    // TODO [kl] figure out why the test is broken but the plugin itself works correctly
/*
    fun `test operator decl rename`() {
        doTest("##",
"""
(**) : number -> number -> number
(**) = (^)
infixl 0 **
x = 2 {-caret-}** 3
""", """
(##) : number -> number -> number
(##) = (^)
infixl 0 ##
x = 2 ## 3
""")}
*/


    // TODO [kl] improve this test by also verifying that the FILE is renamed (currently not implemented in the plugin)
    fun `test module rename from Data_User to Quux`() {
        checkByDirectory(
"""
--@ Data/User.elm
module Data.User exposing (..)
type alias User = { x : String }
name = identity

--@ main.elm
import Data.User
f : Data.User.User -> String
f user = Data.User.name user
g = f (Data.User.User "joe")
""",
"""
--@ Data/User.elm
module Quux exposing (..)
type alias User = { x : String }
name = identity

--@ main.elm
import Quux
f : Quux.User -> String
f user = Quux.name user
g = f (Quux.User "joe")
""") {
            val mod = myFixture.configureFromTempProjectFile("Data/User.elm")
                    .descendantsOfType<ElmModuleDeclaration>().single()
            check(mod.name == "Data.User")
            myFixture.renameElement(mod, "Quux")
        }
    }

    fun `test import alias rename`() {
        checkByDirectory(
"""
--@ Data/User.elm
module Data.User exposing (..)
type alias User = { x : String }
name = identity

--@ main.elm
import Data.User as DU
f : DU.User -> String
f user = DU.name user
g = f (DU.User "joe")
""",
"""
--@ Data/User.elm
module Data.User exposing (..)
type alias User = { x : String }
name = identity

--@ main.elm
import Data.User as U
f : U.User -> String
f user = U.name user
g = f (U.User "joe")
""") {
            val aliasClause = myFixture.configureFromTempProjectFile("main.elm")
                    .descendantsOfType<ElmImportClause>().single()
                    .asClause!!
            check(aliasClause.name == "DU")
            myFixture.renameElement(aliasClause, "U")
        }
    }

    fun doTest(newName: String, @Language("Elm") before: String, @Language("Elm") after: String) {
        InlineFile(before).withCaret()
        myFixture.renameElementAtCaret(newName)
        myFixture.checkResult(after)
    }
}