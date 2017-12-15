package org.elm.lang.core.completion

class ElmKeywordCompletionTest: ElmCompletionTestBase() {

    fun testDummy() {}

/*
    TODO [kl] re-enable once we implement keyword completion
    fun `test 'type' keyword`() = doSingleCompletion(
"""
typ{-caret-}
""", """
type {-caret-}
""")


    fun `test 'alias' keyword`() = doSingleCompletion(
"""
type al{-caret-}
""", """
type alias {-caret-}
""")

    fun `test 'module' keyword`() = doSingleCompletion(
"""
mod{-caret-}
""", """
module {-caret-}
""")

    fun `test 'exposing' keyword in a module`() = doSingleCompletion(
"""
module Foo exp{-caret-}
""", """
module Foo exposing ({-caret-})
""")

    fun `test 'exposing' keyword in an import`() = doSingleCompletion(
"""
import Foo exp{-caret-}
""", """
import Foo exposing ({-caret-})
""")
*/

}