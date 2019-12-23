package org.elm.ide.inspections

class ElmInspectionSuppressorTest : ElmInspectionsTestBase(ElmUnusedSymbolInspection()) {

    fun testWithoutSuppression() = checkByText("""
type T = T ()
<warning>f</warning> = g

g : T -> () -> ()
g t <warning>x</warning> = 
  case t of
      T <warning>u</warning> -> ()
    """)

    fun testSuppression() = checkByText("""
type T = T ()
-- noinspection ElmUnusedSymbol
f = g

-- noinspection ElmUnusedSymbol
g : T -> () -> ()
g t x = 
  case t of
      T u -> ()
    """)
}
