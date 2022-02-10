package org.elm.ide.structure

import com.intellij.testFramework.PlatformTestUtil
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test

internal class ElmStructureViewTest : ElmTestBase() {
    @Test
    fun `test top-level declarations`() = doTest("""
type alias Alias = ()
type Foo = Bar
port somePort : ()
main = 1
""", """
-Main.elm
 Alias
 Foo
 somePort
 main
""")

    @Test
    fun `test nested value declaration`() = doTest("""
main =
  let foo = 1 in foo
""", """
-Main.elm
 -main
  foo
""")

    @Test
    fun `test nested value declarations in tuple`() = doTest("""
main =
  (
  let foo = 1 in foo,
  let bar = 2 in bar
  )
""", """
-Main.elm
 -main
  foo
  bar
""")

    @Test
    fun `test deeply nested value declarations`() = doTest("""
main =
  let
    level1 =
      let
        level2a = 1
        level2b =
          let
            level3a = 1
            level3b = 2
          in
          level3b
      in
      level2b
  in
  level1
""", """
-Main.elm
 -main
  -level1
   level2a
   -level2b
    level3a
    level3b
""")

    @Test
    fun `test nested declaration with destructuring`() = doTest("""
main =
    let (foo, bar) = (1, 2)
    in foo
""", """
-Main.elm
 -main
  foo, bar
""")

    private fun doTest(@Language("Elm") code: String, expected: String) {
        myFixture.configureByText("Main.elm", code)
        myFixture.testStructureView {
            PlatformTestUtil.expandAll(it.tree)
            PlatformTestUtil.assertTreeEqual(it.tree, expected)
        }
    }
}
