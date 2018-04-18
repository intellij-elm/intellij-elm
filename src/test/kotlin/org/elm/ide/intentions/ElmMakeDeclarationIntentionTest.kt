package org.elm.ide.intentions

class ElmMakeDeclarationIntentionTest: ElmIntentionTestBase(ElmMakeDeclarationIntentionAction()) {


    fun `test make value declaration`() {
        doAvailableTest(
"""
f : Int{-caret-}
"""
, """
f : Int
f = {-caret-}
""")}


    fun `test make basic function declaration`() {
        doAvailableTest(
"""
f : Int -> Int{-caret-}
"""
, """
f : Int -> Int
f int = {-caret-}
""")}


    fun `test make advanced function declaration`() {
        doAvailableTest(
"""
f : (Int -> Int) -> List a -> (Char, String) -> { foo : Int } -> Bool{-caret-}
"""
, """
f : (Int -> Int) -> List a -> (Char, String) -> { foo : Int } -> Bool
f function list tuple record = {-caret-}
""")}


}