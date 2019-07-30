package org.elm.ide.lineMarkers

class ElmRecursiveCallLineMarkerProviderTest : ElmLineMarkerProviderTestBase() {
    fun `test top level functions`() = doTestByText(
            """
f a = f (a - 1) --> Recursive call
g a =
  g (a + 1) --> Recursive call
h a = h ( h (a + 1) ) --> Recursive call
""")

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
