package org.elm.ide.inspections.inference

import com.intellij.openapi.vfs.VirtualFileFilter
import org.elm.fileTreeFromText
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.descendantsOfType
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmValueDeclaration
import org.elm.lang.core.psi.elements.ElmValueExpr
import org.elm.lang.core.types.findInference
import org.elm.lang.core.types.findTy
import org.elm.lang.core.types.renderedText
import org.elm.lang.core.types.typeExpressionInference
import org.intellij.lang.annotations.Language

class TypeInferenceStubAccessTest : ElmTestBase() {

    /*
        Type inference should use stubs as much as possible.

        These tests are kinda weird because you need to setup strange situations
        in the imported file in order to exercise the type inference code completely
        over a stub-backed Psi tree. Most of these tests have been discovered by running
        IntelliJ with debug logging enabled for "#com.intellij.psi.impl.source.PsiFileImpl"
        and looking for unexpected "Loaded text for file" log messages when opening files
        and running type inference. Then set a breakpoint where that log message is
        emitted to check the callstack and create a test that recreates the scenario.
        It's tedious and lame, but it works.

        Ideally type inference could operate 100% using stubs. But this is only feasible
        when functions have type annotations, in which case we do not need to peek into
        the body of the function.

        So in the rare case where the user calls an unannotated function from a different
        module, the other module's stubs will be converted to full-AST-backed. The only
        other option is to return `TyUnknown` in such cases, but we have chosen to avoid
        the false negative.
    */


    fun `test infer basic value expr across modules`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo
f : Foo.Bar -> ()
f x = x
    --^Bar

--@ Foo.elm
module Foo exposing (..)
type alias Bar = ()
""")


    fun `test infer function across modules with type annotation`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing (foo)
f = foo
    --^()

--@ Foo.elm
module Foo exposing (..)
foo : ()
foo = ()
""")


    fun `test infer function across modules where type does not exist`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing (foo)
f = foo
    --^unknown

--@ Foo.elm
module Foo exposing (..)
foo : DoesNotExist
foo = 42
""")


    fun `test infer record field across modules`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo
f : Foo.Bar -> ()
f { y } = y
        --^()

--@ Foo.elm
module Foo exposing (..)
type alias Bar =
    { y : () }
""")


    fun `test infer type expr across modules`() = stubOnlyTypeInfer<ElmTypeAliasDeclaration>(
            """
--@ Main.elm
import Foo exposing (Bar)
type alias Thing = Bar
           --^Thing

--@ Foo.elm
module Foo exposing (..)
type alias Bar = ()
""")


    fun `test infer union type with type variable expr`() = stubOnlyTypeInfer<ElmTypeAliasDeclaration>(
            """
--@ Main.elm
import Foo exposing (Bar)
type alias Thing = Bar ()
           --^Thing

--@ Foo.elm
module Foo exposing (..)
type Bar a = Bar a
""")


    fun `test infer with union type variable reference`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing (..)
f = g
  --^()
g = foo () 0

--@ Foo.elm
module Foo exposing (..)
foo : a -> b -> a
foo x y = x
""")


    fun `test infer type via exposed import`() = stubOnlyTypeInfer<ElmTypeAliasDeclaration>(
            """
--@ Main.elm
import Foo exposing (Foo)
type alias Thing = Foo
           --^Thing

--@ Foo.elm
module Foo exposing (..)
import Bar exposing (Bar)
type alias Foo = Bar

--@ Bar.elm
module Bar exposing (..)
type alias Bar = ()
""")


    fun `test infer qualified type expr in other file`() = stubOnlyTypeInfer<ElmTypeAliasDeclaration>(
            """
--@ Main.elm
import Foo exposing (Foo)
type alias Thing = Foo
           --^Thing

--@ Foo.elm
module Foo exposing (..)
import Bar
type alias Foo = Bar.Bar

--@ Bar.elm
module Bar exposing (..)
type alias Bar = ()
""")


    fun `test infer func that destructures union type parameter`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing (..)
f = g
  --^()
g = foo (Foo ())

--@ Foo.elm
module Foo exposing (..)
type Foo = Foo ()
foo : Foo -> ()
foo (Foo x) = x
""")


    fun `test infer infix operator`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing ((**))
f = g
  --^()
g = () ** ()

--@ Foo.elm
module Foo exposing (..)
infix left  7 (**)  = foo
foo : a -> b -> a
foo x y = x
""")


    fun `test infer port annotation`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing (foo)
f = foo
  --^()

--@ Foo.elm
port module Foo exposing (..)
port foo : ()
""")


    fun `test infer function with extensible record param`() = stubOnlyTypeInfer<ElmValueExpr>(
            """
--@ Main.elm
import Foo exposing (foo)
f = g
  --^()
g = foo { id = 0, name = () }

--@ Foo.elm
module Foo exposing (..)
foo : { a | name : () } -> ()
foo { name } = name
""")


    private inline fun <reified T : ElmPsiElement> stubOnlyTypeInfer(@Language("Elm") code: String) {
        val testProject = fileTreeFromText(code)
                .createAndOpenFileWithCaretMarker()

        checkAstNotLoaded(VirtualFileFilter { file ->
            !file.path.endsWith(testProject.fileWithCaret)
        })

        checkExpectedType<T>()
        checkNoInferenceErrors()
    }


    private inline fun <reified T : ElmPsiElement> checkExpectedType() {
        val (expr, expectedType) = findElementAndDataInEditor<T>()

        // TODO [kl] Find a better way to get the ty for any ElmPsiElement.
        val ty = when (expr) {
            is ElmValueExpr -> expr.findTy()
            is ElmTypeAliasDeclaration -> expr.typeExpressionInference().value
            else -> error("not handled")
        }
        val renderedText = ty?.renderedText()
        check(renderedText == expectedType) {
            "Type mismatch. Expected: $expectedType, found: $renderedText ($ty)"
        }
    }


    private fun checkNoInferenceErrors() {
        val diagnostics = myFixture.file.descendantsOfType<ElmValueDeclaration>()
                .flatMap { it.findInference()?.diagnostics ?: emptyList() }
        if (diagnostics.isNotEmpty()) {
            error(
                    diagnostics.joinToString("\n", "Program failed to type check: \n") {
                        "\t${it.message}"
                    }
            )
        }
    }
}