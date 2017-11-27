package org.elm.lang.core.resolve


/**
 * Miscellaneous tests related to the reference-resolve system
 */
class ElmMiscResolveTest: ElmResolveTestBase() {


    fun `test top-level value ref`() = checkByCode(
"""
magicNumber = 42
--X

f = magicNumber + 1
    --^
""")


    fun `test simple value declared by let-in`() = checkByCode(
"""
f x =
    let y = 42
      --X
    in x + y
         --^
""")

}