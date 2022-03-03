/*
The MIT License (MIT)

Derived from intellij-rust
Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
Copyright (c) 2016 JetBrains

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.elm.ide.refactoring

import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.IncorrectOperationException
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.intellij.lang.annotations.Language


class ElmRenameTest : ElmTestBase() {


    fun `test value decl rename`() = doTest("quux", """
foo : Int
foo = 42
bar = foo{-caret-} + 2
""", """
quux : Int
quux = 42
bar = quux + 2
""")

    fun `test value decl invalid rename`() = doInvalidNameTest("Bar", """
foo{-caret-} = 42
""")

    fun `test type decl invalid rename`() = doInvalidNameTest("bar", """
type Foo{-caret-} = Foo
""")

    fun `test function parameter rename`() = doTest("z", """
f x{-caret-} y = x + y
""", """
f z y = z + y
""")


    fun `test let destructuring rename`() = doTest("z", """
f p =
    let (x{-caret-}, y) = p
    in x
""", """
f p =
    let (z, y) = p
    in z
""")


    fun `test case destructuring rename`() = doTest("z", """
f p =
    case p of
        (x{-caret-}, y) -> x
""", """
f p =
    case p of
        (z, y) -> z
""")


    fun `test union type rename`() = doTest("Quux", """
type Foo{-caret-} = A | B
type Bar = C Foo
f : Foo -> String
""", """
type Quux = A | B
type Bar = C Quux
f : Quux -> String
""")


    fun `test type alias rename`() = doTest("Quux", """
type alias Foo{-caret-} = Int
type Bar = C Foo
f : Foo -> String
""", """
type alias Quux = Int
type Bar = C Quux
f : Quux -> String
""")


    fun `test field access rename`() = doTest("quux", """
foo : { b : String }
foo a{-caret-} = a.b
""", """
foo : { b : String }
foo quux = quux.b
""")


    // disabled until https://github.com/klazuka/intellij-elm/issues/240 is fixed
/*
    fun `test file rename from Data_User to Data_Quux`() = checkByDirectory("""
--@ Data/User.elm
module Data.User exposing (..)
type alias User = { x : String }

--@ main.elm
import Data.User
g = Data.User.User "joe"
""", """
--@ Data/Quux.elm
module Data.Quux exposing (..)
type alias User = { x : String }

--@ main.elm
import Data.Quux
g = Data.Quux.User "joe"
""") {
        val file = myFixture.configureFromTempProjectFile("Data/User.elm")
        myFixture.renameElement(file, "Quux")
    }
*/


    fun `test module decl rename from Data_User to Data_Quux`() = checkByDirectory("""
--@ Data/User.elm
module Data.User exposing (..)
type alias User = { x : String }

--@ main.elm
import Data.User
g = Data.User.User "joe"
""", """
--@ Data/Quux.elm
module Data.Quux exposing (..)
type alias User = { x : String }

--@ main.elm
import Data.Quux
g = Data.Quux.User "joe"
""") {
        val mod = myFixture.configureFromTempProjectFile("Data/User.elm")
                .descendantsOfType<ElmModuleDeclaration>().single()
        check(mod.name == "Data.User")
        myFixture.renameElement(mod, "Quux")
    }


    fun `test import alias rename`() = checkByDirectory("""
--@ Data/User.elm
module Data.User exposing (..)
type alias User = { x : String }
name = identity

--@ main.elm
import Data.User as DU
f : DU.User -> String
f user = DU.name user
g = f (DU.User "joe")
""", """
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

    fun doTest(newName: String, @Language("Elm") before: String, @Language("Elm") after: String) {
        InlineFile(before).withCaret()
        myFixture.renameElementAtCaret(newName)
        myFixture.checkResult(after)
    }

    fun doInvalidNameTest(newName: String, @Language("Elm") before: String) {
        InlineFile(before).withCaret()
        try {
            myFixture.renameElementAtCaret(newName)
        } catch (e: RuntimeException) {
            UsefulTestCase.assertInstanceOf(e.cause, IncorrectOperationException::class.java)
            myFixture.checkResult(before.replace("{-caret-}", ""))
            return
        }
        throw AssertionError("exception not thrown")
    }
}
