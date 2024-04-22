package org.elm.lang.core.resolve

import org.junit.Test


/**
 * Tests related to pattern matching and destructuring
 */
class ElmPatternResolveTest : ElmResolveTestBase() {


    // CASE-OF EXPRESSIONS AND PATTERNS


    @Test
    fun `test case-of pattern wildcard`() = checkByCode(
            """
f x =
    case x of
        0 -> 0
        y -> 2 * y
      --X      --^
"""
    )


    @Test
    fun `test case-of pattern union type constructor`() = checkByCode(
            """
f x =
    case x of
        Nothing -> 0
        Just something -> something
             --X          --^
"""
    )

    @Test
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


    @Test
    fun `test case-of that should not resolve`() = checkByCode(
            """
f x =
    case () of
        Just foo -> ()
        _ -> foo
             --^unresolved
"""
    )


    @Test
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


    // see bug https://github.com/intellij-elm/intellij-elm/issues/106
    @Test
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


    @Test
    fun `test function parameter record destructuring`() = checkByCode(
            """
foo { name } = name
      --X      --^
""")


    @Test
    fun `test function parameter tuple destructuring`() = checkByCode(
            """
foo ( x, y ) = x + y
       --X       --^
""")


    @Test
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


    @Test
    fun `test pattern alias in function decl parameter`() = checkByCode(
            """
foo ((x, y) as point) = point
               --X      --^
""")


    @Test
    fun `test pattern alias in function parameter in let-in expr`() = checkByCode(
            """
f =
    let
        g ((x, y) as point) = point
                     --X      --^
    in
        g (320, 480)
""")


    @Test
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


    @Test
    fun `test pattern alias in case-of branch`() = checkByCode(
            """
f x =
    case x of
        ((x, y) as point) -> point
                   --X       --^
""")


}
