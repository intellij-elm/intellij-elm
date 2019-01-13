package org.elm.ide.intentions

class MakeDeclarationIntentionTest : ElmIntentionTestBase(MakeDeclarationIntention()) {

    override fun getProjectDescriptor() = ElmWithStdlibDescriptor


    fun `test make value declaration`() {
        doAvailableTest(
                """
f : Int{-caret-}
"""
                , """
f : Int
f =
    {-caret-}
""")
    }


    fun `test make basic function declaration`() {
        doAvailableTest(
                """
f : Int -> Int{-caret-}
"""
                , """
f : Int -> Int
f int =
    {-caret-}
""")
    }


    fun `test make advanced function declaration`() {
        doAvailableTest(
                """
f : (Int -> Int) -> List a -> (Char, String) -> { foo : Int } -> Bool{-caret-}
"""
                , """
f : (Int -> Int) -> List a -> (Char, String) -> { foo : Int } -> Bool
f function list (char, string) record =
    {-caret-}
""")
    }


    fun `test function parameters should be camelCased`() {
        doAvailableTest(
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
    }


}
