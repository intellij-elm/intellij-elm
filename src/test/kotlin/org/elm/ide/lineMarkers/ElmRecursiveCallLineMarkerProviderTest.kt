package org.elm.ide.lineMarkers

import org.junit.Test


class ElmRecursiveCallLineMarkerProviderTest : ElmLineMarkerProviderTestBase() {
    @Test
    fun `test non-recursive functions`() = doTestByText(
            """
f a = a
g = f
h = [h]
""")
    @Test
    fun `test top level functions`() = doTestByText(
            """
f a = f (a - 1) --> Recursive call
g a =
  g (a + 1) --> Recursive call
h a = h ( h (a + 1) ) --> Recursive call
""")

    @Test
    fun `test nested functions`() = doTestByText(
            """
top t =
  let
    f a = f (a - 1) --> Recursive call
    g a =
      g (a + 1) --> Recursive call
  in
    top t --> Recursive call
""")
}
