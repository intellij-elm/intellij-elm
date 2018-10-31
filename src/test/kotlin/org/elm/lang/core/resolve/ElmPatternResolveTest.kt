package org.elm.lang.core.resolve


/**
 * Tests related to pattern matching and destructuring
 */
class ElmPatternResolveTest : ElmResolveTestBase() {


    // CASE-OF EXPRESSIONS AND PATTERNS


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


    fun `test case-of that should not resolve`() = checkByCode(
            """
f x =
    case () of
        Just foo -> ()
        _ -> foo
             --^unresolved
"""
    )


    fun `test nested case-of`() = checkByCode(
            """
f x =
    case () of
        _ ->
            case () of
                _ -> ()
                Just foo -> foo
                     --X    --^
"""
    )


    // see bug https://github.com/klazuka/intellij-elm/issues/106
    fun `test nested case-of that should not resolve`() = checkByCode(
            """
f x =
    case () of
        _ ->
            case () of
                Just foo -> ()
                _ -> foo
                     --^unresolved
"""
    )


    // PARAMETER DESTRUCTURING


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


    // TODO [drop 0.18] this becomes invalid at the top-level in 0.19
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


    // PATTERN ALIASES


    fun `test pattern alias in function decl parameter`() = checkByCode(
            """
foo ((x, y) as point) = point
               --X      --^
""")


    fun `test pattern alias in let-in decl`() = checkByCode(
            """
f =
    let
        g ((x, y) as point) = point
                     --X      --^
    in
        g (320, 480)
""")


    fun `test pattern alias in case-of branch`() = checkByCode(
            """
f x =
    case x of
        ((x, y) as point) -> point
                   --X       --^
""")


}