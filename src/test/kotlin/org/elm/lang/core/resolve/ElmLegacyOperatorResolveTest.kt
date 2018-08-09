package org.elm.lang.core.resolve

// TODO [drop 0.18] remove this entire file

/**
 * Tests related to resolving Elm 0.18 binary operator references
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
