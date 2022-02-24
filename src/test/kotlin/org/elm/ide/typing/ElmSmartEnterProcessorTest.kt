package org.elm.ide.typing

import com.intellij.openapi.actionSystem.IdeActions
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.indentStyle
import org.intellij.lang.annotations.Language
import org.junit.Test

class ElmSmartEnterProcessorTest : ElmTestBase() {
    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
    @Test
    fun `test function declaration with no params`() = doTest("""
module Main exposing (..)
foo{-caret-}
""", """
module Main exposing (..)
foo =
    {-caret-}
""")

    @Test
    fun `test function declaration with params`() = doTest("""
module Main exposing (..)
foo a b{-caret-}
""", """
module Main exposing (..)
foo a b =
    {-caret-}
""")

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
