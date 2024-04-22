package org.elm.ide.inspections

import org.junit.Test


class ElmTupleSizeInspectionTest : ElmInspectionsTestBase(ElmTupleSizeInspection()) {
    @Test
    fun `test 2-tuple`() = checkByText("""
main = (1, 2)
""")

    @Test
    fun `test 3-tuple`() = checkByText("""
main = (1, 2, 3)
""")

    @Test
    fun `test 4-tuple`() = checkByText("""
main = <error descr="Tuples may only have two or three items.">(1, 2, 3, 4)</error>
""")

    @Test
    fun `test 5-tuple`() = checkByText("""
main = <error descr="Tuples may only have two or three items.">(1, 2, 3, 4, 5)</error>
""")
}
