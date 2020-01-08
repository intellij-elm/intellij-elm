package org.elm.ide.inspections


class ElmTupleSizeInspectionTest : ElmInspectionsTestBase(ElmTupleSizeInspection()) {
    fun `test 2-tuple`() = checkByText("""
main = (1, 2)
""")

    fun `test 3-tuple`() = checkByText("""
main = (1, 2, 3)
""")

    fun `test 4-tuple`() = checkByText("""
main = <error descr="Tuples may only have two or three items.">(1, 2, 3, 4)</error>
""")

    fun `test 5-tuple`() = checkByText("""
main = <error descr="Tuples may only have two or three items.">(1, 2, 3, 4, 5)</error>
""")
}
