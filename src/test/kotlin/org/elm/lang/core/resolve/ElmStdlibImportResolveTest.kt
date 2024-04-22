package org.elm.lang.core.resolve

import org.junit.Test


/**
 * Test resolving values and types provided by the elm/core package.
 *
 * Special emphasis is put on verifying that the implicit, default imports provided by the Elm compiler
 * are handled correctly. For the full list of default imports, see:
 * https://package.elm-lang.org/packages/elm/core/latest/
 */
class ElmStdlibImportResolveTest : ElmResolveTestBase() {

    override fun getProjectDescriptor() =
            ElmWithStdlibDescriptor


    // BASICS MODULE


    @Test
    fun `test Basics module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Basics.toFloat 42
           --^...Basics.elm
""")


    @Test
    fun `test Basics exposes all values`() = stubOnlyResolve(
            """
--@ main.elm
f = toFloat 42
    --^...Basics.elm
""")


    @Test
    fun `test Basics exposes all types`() = stubOnlyResolve(
            """
--@ main.elm
type alias Config = { ordering : Order }
                                 --^...Basics.elm
""")


    @Test
    fun `test Basics exposes all constructors`() = stubOnlyResolve(
            """
--@ main.elm
f = LT
   --^...Basics.elm
""")


    @Test
    fun `test Basics exposes binary operators`() = stubOnlyResolve(
            """
--@ main.elm
f = 2 + 2
    --^...Basics.elm
""")


    @Test
    fun `test Basics can be shadowed by local definitions`() = stubOnlyResolve(
            """
--@ main.elm
and a b = 0
f = and 1 1
    --^...main.elm
""")


    @Test
    fun `test Basics can be shadowed by explicit imports`() = stubOnlyResolve(
            """
--@ main.elm
import Bitwise exposing (and)
f = and 1 1
    --^...Bitwise.elm
""")


    // LIST MODULE


    @Test
    fun `test List module imported`() = stubOnlyResolve(
            """
--@ main.elm
n = List.length [0, 1, 2]
    --^...List.elm
""")


    @Test
    fun `test List cons op exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = 0 :: []
    --^...List.elm
""")


    @Test
    fun `test List doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = foldl (+) [0,1,2]
    --^unresolved
""")


    // MAYBE MODULE


    @Test
    fun `test Maybe module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = Maybe.withDefault x 42
       --^...Maybe.elm
""")


    @Test
    fun `test Maybe type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Maybe Int
                 --^...Maybe.elm
""")


    @Test
    fun `test Maybe Just exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Just 42
    --^...Maybe.elm
""")


    @Test
    fun `test Maybe Nothing exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Nothing
    --^...Maybe.elm
""")


    @Test
    fun `test Maybe doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = withDefault
    --^unresolved
""")


    // RESULT MODULE


    @Test
    fun `test Result module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = Result.withDefault x 42
       --^...Result.elm
""")


    @Test
    fun `test Result type exposed (flaky)`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Result String Int
                 --^...Result.elm
""")


    @Test
    fun `test Result Ok exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Ok 42
   --^...Result.elm
""")


    @Test
    fun `test Result Err exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Err "uh oh"
    --^...Result.elm
""")


    @Test
    fun `test Result module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f x = toMaybe x 42
      --^unresolved
""")


    // STRING MODULE


    @Test
    fun `test String module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = String.length x
       --^...String.elm
""")


    @Test
    fun `test String type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = String
                 --^...String.elm
""")


    @Test
    fun `test String module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = length "hello"
      --^unresolved
""")


    // CHAR MODULE


    @Test
    fun `test Char module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = Char.isUpper x
      --^...Char.elm
""")


    @Test
    fun `test Char type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Char
                 --^...Char.elm
""")


    @Test
    fun `test Char module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = isUpper 'A'
      --^unresolved
""")


    // TUPLE MODULE


    @Test
    fun `test Tuple module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Tuple.first (0, 0)
      --^...Tuple.elm
""")


    @Test
    fun `test Tuple module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = pair 0 0
    --^unresolved
""")


    // DEBUG MODULE


    @Test
    fun `test Debug module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Debug.toString
    --^...Debug.elm
""")


    @Test
    fun `test Debug module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = toString
    --^unresolved
""")


    // PLATFORM MODULE


    @Test
    fun `test Platform module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Platform.worker
    --^...Platform.elm
""")


    @Test
    fun `test Platform module exposes Program type`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Program
                 --^...Platform.elm
""")


    @Test
    fun `test Platform module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = worker
    --^unresolved
""")


    // PLATFORM.CMD MODULE


    @Test
    fun `test Platform Cmd module imported using Cmd alias (module ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Cmd.none
    --^...Platform/Cmd.elm
""")


    @Test
    fun `test Platform Cmd module imported using Cmd alias (value ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Cmd.none
        --^...Platform/Cmd.elm
""")


    @Test
    fun `test Platform Cmd module exposes Cmd type`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Cmd
                 --^...Platform/Cmd.elm
""")


    @Test
    fun `test Platform Cmd module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = batch
    --^unresolved
""")


    // PLATFORM.SUB MODULE


    @Test
    fun `test Platform Sub module imported using Sub alias (module ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Sub.none
    --^...Platform/Sub.elm
""")


    @Test
    fun `test Platform Sub module imported using Sub alias (value ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Sub.none
        --^...Platform/Sub.elm
""")


    @Test
    fun `test Platform Sub module exposes Sub type`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Sub
                 --^...Platform/Sub.elm
""")


    @Test
    fun `test Platform Sub module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = batch
    --^unresolved
""")


}