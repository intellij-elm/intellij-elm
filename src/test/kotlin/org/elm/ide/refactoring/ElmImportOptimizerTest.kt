package org.elm.ide.refactoring

import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

class ElmImportOptimizerTest : ElmTestBase() {


    fun `test do nothing if no imports at all`() =
            doTest(
                    """
--@ Main.elm
main = ()
--^

--@ Foo.elm
module Foo exposing (..)
foo = 42
""",
                    """
main = ()
--^
""")


    fun `test removes unused imports`() =
            doTest(
                    """
--@ Main.elm
import Bar
import Foo
import Quux exposing (quux)
main = Bar.bar + Quux.quux
--^

--@ Foo.elm
module Foo exposing (..)
foo = 1

--@ Bar.elm
module Bar exposing (..)
bar = 2

--@ Quux.elm
module Quux exposing (..)
quux = 3
""",
                    """
import Bar
import Quux
main = Bar.bar + Quux.quux
--^
""")


    /*
    For more detailed tests related to DETECTING unused imports, see [ElmUnusedImportInspectionTest]
    */


    private fun doTest(@Language("Elm") before: String, @Language("Elm") after: String) {
        configureByFileTree(before)
        myFixture.performEditorAction("OptimizeImports")
        myFixture.checkResult(after.trimStart())
    }
}