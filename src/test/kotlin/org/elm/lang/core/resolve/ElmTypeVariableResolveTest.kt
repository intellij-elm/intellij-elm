package org.elm.lang.core.resolve

import org.junit.Test


class ElmTypeVariableResolveTest : ElmResolveTestBase() {
    @Test
    fun `test return value to param`() = checkByCode(
            """
foo : a -> a
    --X  --^
foo a = a
""")

    @Test
    fun `test param to param`() = checkByCode(
            """
foo : a -> b -> a -> ()
    --X       --^
foo _ _ _ = ()
""")

    @Test
    fun `test function param to top level param`() = checkByCode(
            """
foo : a -> (a -> a) -> ()
    --X        --^
foo _ _ = ()
""")

    @Test
    fun `test record field to top level param`() = checkByCode(
            """
foo : a -> { f : a } -> ()
    --X        --^
foo _ _ = ()
""")

    @Test
    fun `test nested annotation return value to param`() = checkByCode(
            """
foo : a -> ()
foo _ =
    let
        bar : b -> b
            --X  --^
        bar b = b
    in
        ()
""")

    @Test
    fun `test nested annotation param to outer param 1`() = checkByCode(
            """
foo : a -> ()
    --X
foo _ =
    let
        bar : a -> a -> ()
            --^
        bar b = b
    in
        ()
""")

    @Test
    fun `test nested annotation param to outer param 2`() = checkByCode(
            """
foo : a -> ()
    --X
foo _ =
    let
        bar : a -> a -> ()
                 --^
        bar b = b
    in
        ()
""")

    @Test
    fun `test nested record field to outer param`() = checkByCode(
            """
foo : b -> a -> ()
         --X
foo _ =
    let
        bar : a -> { f : { g : a } }
                             --^
        bar b = b
    in
        ()
""")
}
