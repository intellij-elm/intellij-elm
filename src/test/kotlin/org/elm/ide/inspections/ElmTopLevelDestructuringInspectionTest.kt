package org.elm.ide.inspections

import org.junit.Test


class ElmTopLevelDestructuringInspectionTest : ElmInspectionsTestBase(ElmTopLevelDestructuringInspection()) {

    @Test
    fun `test top-level destructuring is forbidden (flaky)`() = checkByTextWithHighlighting("""
module Main exposing (..)
<error descr="Destructuring at the top-level is not allowed">(x,y)</error> = (0,0)
""")

    @Test
    fun `test nested destructuring is permitted (flaky)`() = checkByText("""
module Main exposing (..)
main =
    let
        (x,y) = (0,0)
    in
    x
""")
}
