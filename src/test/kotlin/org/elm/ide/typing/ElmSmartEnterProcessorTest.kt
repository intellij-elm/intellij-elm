package org.elm.ide.typing

import com.intellij.openapi.actionSystem.IdeActions
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.indentStyle
import org.intellij.lang.annotations.Language

class ElmSmartEnterProcessorTest : ElmTestBase() {
    fun `test case expression`() = doTest("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of{-caret-}
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Bar ->
            {-caret-}

        Baz ->
            --EOL

        Qux ->

""")

    fun `test single case branch `() = doTest("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Bar{-caret-}
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
    case it of
        Bar ->
            {-caret-}
""")

    fun `test let with no body`() = doTest("""
foo =
    let{-caret-}
""", """
foo =
    let
        {-caret-}
    in
        --EOL
""")

    fun `test nested let with no body and no parent in-keyword`() = doTest("""
foo =
    let
        bar =
            let{-caret-}
""", """
foo =
    let
        bar =
            let
                {-caret-}
            in
                --EOL
""")

    fun `test if with no then`() = doTest("""
foo =
    if True{-caret-}
""", """
foo =
    if True then
        {-caret-}
    else
        --EOL
""")

    fun `test if with no body`() = doTest("""
foo =
    if True then{-caret-}
""", """
foo =
    if True then
        {-caret-}
    else
        --EOL
""")

    fun `test if with no body, caret before then`() = doTest("""
foo =
    if True{-caret-} then
""", """
foo =
    if True then
        {-caret-}
    else
        --EOL
""")

    fun `test if with no else`() = doTest("""
foo =
    if True then
        1{-caret-}
""", """
foo =
    if True then
        1
    else
        {-caret-}
""")

    fun `test chained if with no then`() = doTest("""
foo =
    if True then
        1
    else if True{-caret-}
""", """
foo =
    if True then
        1
    else if True then
        {-caret-}
    else
        --EOL
""")

    fun `test chained if with no else`() = doTest("""
foo =
    if True then
        1
    else if True then
        2{-caret-}
""", """
foo =
    if True then
        1
    else if True then
        2
    else
        {-caret-}
""")

    // Need module statement to prevent `foo` from being parsed inside an error element
    fun `test function declaration with no params`() = doTest("""
module Main exposing (..)
foo{-caret-}
""", """
module Main exposing (..)
foo =
    {-caret-}
""")

    fun `test function declaration with params`() = doTest("""
module Main exposing (..)
foo a b{-caret-}
""", """
module Main exposing (..)
foo a b =
    {-caret-}
""")

    fun `test nested function declaration with no params`() = doTest("""
foo =
    let
        bar{-caret-}
    in
        ()
""", """
foo =
    let
        bar =
            {-caret-}
    in
        ()
""")

    fun `test nested function declaration with params`() = doTest("""
foo =
    let
        bar a b{-caret-}
    in
        ()
""", """
foo =
    let
        bar a b =
            {-caret-}
    in
        ()
""")

    fun `test nested tuple destructuring with no params`() = doTest("""
foo =
    let
        (a, b){-caret-}
    in
        ()
""", """
foo =
    let
        (a, b) =
            {-caret-}
    in
        ()
""")

    fun `test nested record destructuring with no params`() = doTest("""
foo =
    let
        {a, b}{-caret-}
    in
        ()
""", """
foo =
    let
        {a, b} =
            {-caret-}
    in
        ()
""")

    fun `test case expression with custom indent`() = checkByText("""
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
  case it of{-caret-}
""", """
type Foo = Bar | Baz | Qux

foo : Foo -> ()
foo it =
  case it of
    Bar ->
      {-caret-}

    Baz ->
      --EOL

    Qux ->

""".replace("--EOL", "")) {
        myFixture.file.indentStyle.INDENT_SIZE = 2
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT)
    }

    private fun doTest(@Language("Elm") before: String, @Language("Elm") after: String) {
        // We use the --EOL marker to avoid editors trimming trailing whitespace, which is
        // significant for this test.
        checkByText(before, after.replace("--EOL", "")) {
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT)
        }
    }
}
