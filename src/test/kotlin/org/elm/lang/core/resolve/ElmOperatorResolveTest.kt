package org.elm.lang.core.resolve

/**
 * Tests related to resolving binary operator references
 *
 */
class ElmOperatorResolveTest : ElmResolveTestBase() {

    fun `test basic usage`() = checkByCode(
"""
power a b = List.product (List.repeat b a)
infix right 5 (**) = power
              --X

f = 2 ** 3
    --^
""")


    fun `test ref from operator to implementation`() = checkByCode(
"""
infix right 5 (**) = power
                     --^

power a b = 42
--X
""")


    fun `test operator as function`() = checkByCode(
"""
infix right 5 (**) = power
              --X
f = (**) 2 3
    --^
""")


    fun `test exposed by module`() = checkByCode(
"""
module Foo exposing ((**))
                     --^
infix right 5 (**) = power
              --X

power a b = 42
""")

}
