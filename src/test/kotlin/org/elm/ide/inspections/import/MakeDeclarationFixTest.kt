package org.elm.ide.inspections.import

import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmUnresolvedReferenceInspection

class MakeDeclarationFixTest : ElmInspectionsTestBase(ElmUnresolvedReferenceInspection()) {

    override fun getProjectDescriptor() = ElmWithStdlibDescriptor


    fun `test make value declaration`() = checkFixByText("Create",
            """
f : Int{-caret-}
"""
            , """
f : Int
f =
    {-caret-}
""")


    fun `test make basic function declaration`() = checkFixByText("Create",
            """
f : Int{-caret-} -> Int
"""
            , """
f : Int -> Int
f int =
    {-caret-}
""")


    fun `test make advanced function declaration`() = checkFixByText("Create",
            """
f : (Int -> Int) -> List a -> (Char, String) -> { foo : Int } -> Bool{-caret-}
"""
            , """
f : (Int -> Int) -> List a -> (Char, String) -> { foo : Int } -> Bool
f function list (char, string) record =
    {-caret-}
""")


    fun `test function parameters should be camelCased`() = checkFixByText("Create",
            """
type FooBar = FooBar
type QuuxQuuxQuux = QuuxQuuxQuux
f : FooBar -> QuuxQuuxQuux -> Int{-caret-}
"""
            , """
type FooBar = FooBar
type QuuxQuuxQuux = QuuxQuuxQuux
f : FooBar -> QuuxQuuxQuux -> Int
f fooBar quuxQuuxQuux =
    {-caret-}
""")


    // https://github.com/klazuka/intellij-elm/issues/232
    fun `test trailing whitespace does not mess up the generated code`() = checkFixByText("Create",
            """
f : Int -> Int{-caret-}  --end-of-line
"""
            , """
f : Int -> Int  --end-of-line
f int =
    {-caret-}
""")

    fun `test make nested value declaration`() = checkFixByText("Create",
            """
f =
    let
        g : Int{-caret-}
    in
        ()
"""
            , """
f =
    let
        g : Int
        g =
            {-caret-}
    in
        ()
""")


    fun `test list parameters uses plural noun`() = checkFixByText("Create",
            """
type Color = Red | Green | Blue
type alias User = { name : String }
f : List User -> List Color -> Int{-caret-}
"""
            , """
type Color = Red | Green | Blue
type alias User = { name : String }
f : List User -> List Color -> Int
f users colors =
    {-caret-}
""")

    fun `test maybe parameters`() = checkFixByText("Create",
            """
type Color = Red | Green | Blue
type alias User = { name : String }
f : Maybe User -> Maybe Color -> Int{-caret-}
"""
            , """
type Color = Red | Green | Blue
type alias User = { name : String }
f : Maybe User -> Maybe Color -> Int
f maybeUser maybeColor =
    {-caret-}
""")
}
