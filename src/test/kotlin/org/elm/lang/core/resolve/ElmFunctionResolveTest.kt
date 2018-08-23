package org.elm.lang.core.resolve


/**
 * Tests related to resolving function and function parameter references
 */
class ElmFunctionResolveTest : ElmResolveTestBase() {


    fun `test function name ref`() = checkByCode(
"""
addOne x = x + 1
--X

f = addOne 42
    --^
""")


    fun `test function parameter ref`() = checkByCode(
"""
foo x y =  x + y
    --X      --^
""")


    fun `test type annotation refers to function name decl`() = checkByCode(
"""
addOne : Int -> Int
--^
addOne x = x + 1
--X
""")


    fun `test nested function parameter ref`() = checkByCode(
"""
f x =
    let scale y = 100 * y
            --X       --^
    in x
""")


    fun `test deep lexical scope of function parameters`() = checkByCode(
"""
f x =
--X
    let
        y =
            let
                z = x + 1
                  --^
            in z
    in y
""")


    fun `test name shadowing basic`() = checkByCode(
"""
f x =
    let x = 42
      --X
    in x
     --^
""")


    fun `test name shadowing within let-in decls`() = checkByCode(
"""
f x =
    let
        x = 42
      --X
        y = x + 1
          --^
    in
        x
""")


    fun `test recursive function ref`() = checkByCode(
"""
foo x =
--X
    if x <= 0 then 0 else foo (x - 1)
                          --^
""")


    fun `test nested recursive function ref`() = checkByCode(
"""
foo =
    let
        bar y = if y <= 0 then 0 else bar (y - 1)
        --X                           --^
    in bar 100
""")


    fun `test unresolved ref to function`() = checkByCode(
"""
f x = g x
    --^unresolved
""")


    fun `test unresolved ref to function parameter`() = checkByCode(
"""
f x = x
g y = x
    --^unresolved
""")

    fun `test type annotation name ref`() = checkByCode(
            """
foo : Int -> Int
--^
foo a = a
--X

outer =
    let
        foo a = a
    in foo
""")

    fun `test nested type annotation name ref`() = checkByCode(
            """
foo a = a

outer =
    let
        foo : Int -> Int
        --^
        foo a = a
        --X
    in foo
""")
}
