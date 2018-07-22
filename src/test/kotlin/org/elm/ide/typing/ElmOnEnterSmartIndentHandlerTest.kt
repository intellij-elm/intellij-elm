package org.elm.ide.typing

import org.intellij.lang.annotations.Language


class ElmOnEnterSmartIndentHandlerTest: ElmTypingTestBase() {


    fun `test after function equals at EOF`() = doTest("""
f ={-caret-}
""", """
f =
    {-caret-}
""")


    fun `test after function equals before blank line`() = doTest("""
f ={-caret-}

""", """
f =
    {-caret-}

""")


    fun `test after let beginning`() = doTest("""
f =
    let{-caret-}
""", """
f =
    let
        {-caret-}
""")


    fun `test after let partial`() = doTest("""
f =
    let{-caret-}
        x = 0
    in
        x
""", """
f =
    let
        {-caret-}
        x = 0
    in
        x
""")


    fun `test after equals in let declaration beginning`() = doTest("""
f =
    let
        x ={-caret-}
""", """
f =
    let
        x =
            {-caret-}
""")


    fun `test after equals in let declaration partial`() = doTest("""
f =
    let
        x = {-caret-}
    in
        x
""", """
f =
    let
        x = 
            {-caret-}
    in
        x
""")


    fun `test after case-of beginning`() = doTest("""
f x =
    case x of{-caret-}
""", """
f x =
    case x of
        {-caret-}
""")


    fun `test after case branch`() = doTest("""
f x =
    case x of
        Nothing ->{-caret-}
""", """
f x =
    case x of
        Nothing ->
            {-caret-}
""")


    fun `test after equals in a type alias declaration`() = doTest("""
type alias Foo ={-caret-}
""", """
type alias Foo =
    {-caret-}
""")


    fun `test no smart indent BEFORE equals`() = doTest("""
f{-caret-} =
""", """
f
 {-caret-}=
""")


    fun `test no smart indent after equals in a record value expression`() = doTest("""
f =
    { foo ={-caret-}
""", """
f =
    { foo =
    {-caret-}
""")


    fun `test no smart indent after arrow in type annotation`() = doTest("""
f : Int ->{-caret-}
""", """
f : Int ->
{-caret-}
""")


    fun `test no smart indent after equals in a union type declaration`() = doTest("""
type Foo ={-caret-}
""", """
type Foo =
{-caret-}
""")


    fun doTest(@Language("Elm") before: String, @Language("Elm") after: String) {
        InlineFile(before).withCaret()
        myFixture.type('\n')
        myFixture.checkResult(replaceCaretMarker(after))
    }
}
