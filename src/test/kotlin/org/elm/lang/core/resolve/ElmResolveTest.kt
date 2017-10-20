package org.elm.lang.core.resolve


class ElmResolveTest: ElmResolveTestBase() {

    fun testFunctionParameter() = checkByCode("""
        foo x y =
            --X
            x + y
              --^
    """)

    fun testUnionTypeInTypeAnnotation() = checkByCode("""
        type Page = Home
           --X

        view : Page -> Html msg
             --^
    """)
}