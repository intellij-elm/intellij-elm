package org.elm.ide.template

import com.intellij.openapi.actionSystem.IdeActions
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test

class ElmLiveTemplatesTest : ElmTestBase() {
    @Test
    fun `test module`() = expandSnippet(
            """
mod{-caret-}
""",
            """
module main exposing (..)


""")

    @Test
    fun `test module in expression`() = noSnippet(
            """
main =
  mod{-caret-}
""")

    @Test
    fun `test module in comment`() = noSnippet(
            """
{-
mod{-caret-}
-}
""")

    @Test
    fun `test fn1`() = expandSnippet(
            """
fn1{-caret-}
""", """
 :  -> 
  =
    
""")

    @Test
    fun `test fn1 in let`() = expandSnippet(
            """
main =
  let
    fn1{-caret-}
  in
  ()
""", """
main =
  let
     :  -> 
      =
        
  in
  ()
""")

    @Test
    fun `test fn2`() = expandSnippet(
            """
fn2{-caret-}
""", """
 :  ->  -> 
   =
    
""")

    @Test
    fun `test fn3`() = expandSnippet(
            """
fn3{-caret-}
""", """
 :  ->  ->  -> 
    =
    
""")

    @Test
    fun `test ty`() = expandSnippet(
            """
ty{-caret-}
""", """
type 
    = 
""")

    @Test
    fun `test tya`() = expandSnippet(
            """
tya{-caret-}
""", """
type alias  =
   
""")

    @Test
    fun `test let1`() = expandSnippet(
            """
main =
  let1{-caret-}
""", """
main =
  let
       =
          
  in
      
""")

    @Test
    fun `test let1 in binary expression`() = expandSnippet(
            """
main =
  1 + let1{-caret-}
""", """
main =
  1 + let
       =
          
  in
      
""")

    @Test
    fun `test let1 in comment`() = noSnippet(
            """
main =
  --let1{-caret-}
""")

    @Test
    fun `test let1 in string`() = noSnippet(
            """
main = ${"\"\"\""}
    let1{-caret-}
    ${"\"\"\""}
""")

    @Test
    fun `test let1 in params`() = noSnippet(
            """
main let1{-caret-} = ()
""")

    @Test
    fun `test let1 in type expr`() = noSnippet(
            """
main : let1{-caret-}
main = ()
""")

    @Test
    fun `test let1 at top level`() = noSnippet(
            """
let1{-caret-}
""")

    @Test
    fun `test let1 at in nested statement`() = noSnippet(
            """
main =
  let
    let1{-caret-}
  in
  ()
""")

    @Test
    fun `test let1 in let-in expr`() = expandSnippet(
            """
main =
  let
    f=()
  in
  let1{-caret-}
""","""
main =
  let
    f=()
  in
  let
       =
          
  in
      
""")

    @Test
    fun `test let1 in nested expression`() = expandSnippet(
            """
main =
  let
    f = 
      let1{-caret-}
  in
  ()
""","""
main =
  let
    f = 
      let
           =
              
      in
          
  in
  ()
""")

    @Test
    fun `test case1`() = expandSnippet(
            """
main =
  case1{-caret-}
""", """
main =
  case  of
       -> 
          
""")

    private fun expandSnippet(@Language("Elm") before: String, @Language("Elm") after: String) =
            checkByText(before, after) {
                myFixture.performEditorAction(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_BY_TAB)
            }

    private fun noSnippet(@Language("Elm") code: String) = expandSnippet(code, code)
}
