package org.elm.lang.core.resolve


class ElmGlobalImportResolveTest: ElmResolveTestBase() {

    // TODO [kl] re-enable these tests once we can arrange to download Elm Core package
    // and configure a custom [LightProjectDescriptor] that includes Core as standard library.

    // TODO [kl] expand the tests to include additional modules that the Elm compiler includes implicitly.

    fun testDummy() {}

/*

    // Basics Module

    fun testBasicsModuleImported() = stubOnlyResolve(
"""
--@ main.elm
f = Basics.toString 42
           --^ ...Basics.elm
""")

    fun testBasicsExposesAllValues() = stubOnlyResolve(
"""
--@ main.elm
f = toString 42
    --^ ...Basics.elm
""")

    fun testBasicsExposesAllTypes() = stubOnlyResolve(
"""
--@ main.elm
type alias Config = { ordering : Order }
                                 --^ ...Basics.elm
""")

    fun testBasicsExposesAllConstructors() = stubOnlyResolve(
"""
--@ main.elm
f = LT
   --^ ...Basics.elm
""")



    // List Module

    fun testListModuleImported() = stubOnlyResolve(
"""
--@ main.elm
type alias Model = { ids : List Int }
                           --^ ...List.elm
""")

    fun testListTypeExposed() = stubOnlyResolve(
"""
--@ main.elm
type alias Model = { ids : List Int }
                           --^ ...List.elm
""")

    fun testListConsOpExposed() = stubOnlyResolve(
"""
--@ main.elm
f = 0 :: []
    --^ ...List.elm
""")



    // Maybe Module

    fun testMaybeModuleImported() = stubOnlyResolve(
"""
--@ main.elm
f x = Maybe.withDefault x 42
            --^ ...Maybe.elm
""")

    fun testMaybeTypeExposed() = stubOnlyResolve(
"""
--@ main.elm
type alias Model = { id : Maybe Int }
                          --^ ...Maybe.elm
""")

    fun testMaybeJustExposed() = stubOnlyResolve(
"""
--@ main.elm
f = Just 42
    --^ ...Maybe.elm
""")

    fun testMaybeNothingExposed() = stubOnlyResolve(
"""
--@ main.elm
f = Nothing
    --^ ...Maybe.elm
""")
*/



}