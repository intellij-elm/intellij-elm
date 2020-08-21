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

    fun `test case-of pattern union type constructor with constructor parameter`() = checkByCode(
            """
type Foo = Foo
           --X
f x =
    case x of
        Nothing -> 0
        Just Foo -> 1
             --^
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


    fun `test pattern alias in function parameter in let-in expr`() = checkByCode(
            """
f =
    let
        g ((x, y) as point) = point
                     --X      --^
    in
        g (320, 480)
""")


    fun `test pattern alias in let-in destructuring assignment`() = checkByCode(
            """
f =
    let
        ((x, y) as point) = (320, 480)
                   --X
    in
        point
        --^
""")


    fun `test pattern alias in case-of branch`() = checkByCode(
            """
f x =
    case x of
        ((x, y) as point) -> point
                   --X       --^
""")


}
