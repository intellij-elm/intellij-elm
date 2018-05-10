package org.elm.lang.core.resolve

/**
 * Tests related to resolving binary operator references
 *
 * TODO [kl] delete once we drop support for Elm 0.18
 */
class ElmLegacyOperatorResolveTest : ElmResolveTestBase() {

    fun `test basic usage`() = checkByCode(
"""
(**) a b = a ^ b
--X

f = 2 ** 3
    --^
""")


    fun `test operator as function`() = checkByCode(
"""
(**) a b = a ^ b
--X

f = (**) 2 3
    --^
""")


    fun `test type annotation`() = checkByCode(
"""
(**) : number -> number -> number
--^
(**) a b = a ^ b
--X
""")


    fun `test operator config`() = checkByCode(
"""
(**) a b = a ^ b
--X

infixl 0 **
        --^
""")


    fun `test exposed by module`() = checkByCode(
"""
module Foo exposing ((**))
                     --^
(**) a b = a ^ b
--X
""")

}
