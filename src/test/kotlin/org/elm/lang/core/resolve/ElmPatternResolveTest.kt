package org.elm.lang.core.resolve


/**
 * Tests related to pattern matching and destructuring
 */
class ElmPatternResolveTest: ElmResolveTestBase() {


    fun `test case-of pattern wildcard`() = checkByCode(
"""
f x =
    case x of
        0 -> 0
        y -> 2 * y
      --X      --^
"""
    )


    fun `test case-of pattern union type constructor`() = checkByCode(
"""
f x =
    case x of
        Nothing -> 0
        Just something -> something
             --X          --^
"""
    )


    fun `test function parameter record destructuring`() = checkByCode(
"""
foo { name } = name
      --X      --^
""")


    fun `test function parameter tuple destructuring`() = checkByCode(
"""
foo ( x, y ) = x + y
       --X       --^
""")


    fun `test top-level value destructuring`() = checkByCode(
"""
( x, y ) = (0, 0)
   --X

f = y + 20
  --^
""")


    fun `test nested function parameter destructuring`() = checkByCode(
"""
f =
    let
        g ( x, y ) = x + y
             --X       --^
    in
        g (320, 480)
""")

}