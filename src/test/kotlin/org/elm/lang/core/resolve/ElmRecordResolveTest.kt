package org.elm.lang.core.resolve

import org.junit.Test


/**
 * Tests related to resolving record and field references
 *
 * NOTE: as of 2017-12-11 most of this has not yet been implemented.
 */
class ElmRecordResolveTest : ElmResolveTestBase() {

    @Test
    fun `test field access ref`() = checkByCode(
            """
foo : { b : String }
foo a = a.b
  --X --^
""")

    @Test
    fun `test record name base ref`() = checkByCode(
            """
foo a = { a | bar = a.bar }
  --X   --^
""")

    @Test
    fun `test record extension type base ref in type alias decl`() = checkByCode(
            """
type alias Foo a = { a | bar : Int }
             --X   --^
""")

    @Test
    fun `test record extension type base ref in union type decl`() = checkByCode(
            """
type Foo a = Bar { a | bar : Int }
       --X       --^
""")

}
