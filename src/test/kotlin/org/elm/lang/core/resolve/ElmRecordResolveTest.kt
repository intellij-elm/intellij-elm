package org.elm.lang.core.resolve


/**
 * Tests related to resolving record and field references
 *
 * NOTE: as of 2017-12-11 most of this has not yet been implemented.
 */
class ElmRecordResolveTest : ElmResolveTestBase() {


    fun `test field access ref`() = checkByCode(
"""
foo : { b : String }
foo a = a.b
  --X --^
""")


}