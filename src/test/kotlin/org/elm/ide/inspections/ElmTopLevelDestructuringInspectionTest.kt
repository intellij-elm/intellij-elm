package org.elm.ide.inspections


class ElmTopLevelDestructuringInspectionTest : ElmInspectionsTestBase(ElmTopLevelDestructuringInspection()) {

    fun `test top-level destructuring is forbidden`() = checkByText("""
module Main exposing (..)
<error descr="Destructuring at the top-level is not allowed">(x,y)</error> = (0,0)
""")

    fun `test nested destructuring is permitted`() = checkByText("""
module Main exposing (..)
main =
    let
        (x,y) = (0,0)
    in
    x
""")
}
