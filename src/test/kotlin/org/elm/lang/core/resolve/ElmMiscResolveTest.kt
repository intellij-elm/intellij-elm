package org.elm.lang.core.resolve

import org.junit.Test


/**
 * Miscellaneous tests related to the reference-resolve system
 */
class ElmMiscResolveTest : ElmResolveTestBase() {


    @Test
    fun `test top-level value ref`() = checkByCode(
            """
magicNumber = 42
--X

f = magicNumber + 1
    --^
""")


    // LET-IN EXPRESSIONS


    @Test
    fun `test simple value declared by let-in`() = checkByCode(
            """
f x =
    let y = 42
      --X
    in x + y
         --^
""")


    @Test
    fun `test let-in should honor lexical scope in body expr`() = checkByCode(
            """
foo =
    let
        bar y = 0
    in y
     --^unresolved
""")


    @Test
    fun `test let-in should honor lexical scope in sibling decl`() = checkByCode(
            """
foo =
    let
        bar y = 0
        quux = y
             --^unresolved
    in
        quux
""")


    // LAMBDAS (ANONYMOUS FUNCTIONS)


    @Test
    fun `test lambda parameter ref`() = checkByCode(
            """
f = \x -> x
   --X  --^
""")

    @Test
    fun `test lambda parameter nested`() = checkByCode(
            """
f = \x -> (\() -> x)
   --X          --^
""")


    @Test
    fun `test lambda parameter nested and should not resolve`() = checkByCode(
            """
f = \() -> x (\x -> ())
         --^unresolved
""")


    @Test
    fun `test lambda parameter destructured record field ref`() = checkByCode(
            """
f = \{x} -> x
    --X   --^
""")


    @Test
    fun `test lambda parameter destructured tuple ref`() = checkByCode(
            """
f = \(x,y) -> x
    --X     --^
""")

    @Test
    fun `test lambda parameter destructured with alias`() = checkByCode(
            """
f = \((x,y) as point) -> point
               --X       --^
""")


    // PORTS


    @Test
    fun `test port ref`() = checkByCode(
            """
port module Ports exposing (..)
port foo : String -> Cmd msg
     --X

update msg model = (model, foo "blah")
                           --^
""")

}