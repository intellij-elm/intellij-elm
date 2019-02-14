package org.elm.lang.core.resolve


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


    fun `test Basics module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Basics.toFloat 42
           --^...Basics.elm
""")


    fun `test Basics exposes all values`() = stubOnlyResolve(
            """
--@ main.elm
f = toFloat 42
    --^...Basics.elm
""")


    fun `test Basics exposes all types`() = stubOnlyResolve(
            """
--@ main.elm
type alias Config = { ordering : Order }
                                 --^...Basics.elm
""")


    fun `test Basics exposes all constructors`() = stubOnlyResolve(
            """
--@ main.elm
f = LT
   --^...Basics.elm
""")


    fun `test Basics exposes binary operators`() = stubOnlyResolve(
            """
--@ main.elm
f = 2 + 2
    --^...Basics.elm
""")


    fun `test Basics can be shadowed by local definitions`() = stubOnlyResolve(
            """
--@ main.elm
and a b = 0
f = and 1 1
    --^...main.elm
""")


    fun `test Basics can be shadowed by explicit imports`() = stubOnlyResolve(
            """
--@ main.elm
import Bitwise exposing (and)
f = and 1 1
    --^...Bitwise.elm
""")


    // LIST MODULE


    fun `test List module imported`() = stubOnlyResolve(
            """
--@ main.elm
n = List.length [0, 1, 2]
    --^...List.elm
""")


    fun `test List cons op exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = 0 :: []
    --^...List.elm
""")


    fun `test List doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = foldl (+) [0,1,2]
    --^unresolved
""")


    // MAYBE MODULE


    fun `test Maybe module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = Maybe.withDefault x 42
       --^...Maybe.elm
""")


    fun `test Maybe type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Maybe Int
                 --^...Maybe.elm
""")


    fun `test Maybe Just exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Just 42
    --^...Maybe.elm
""")


    fun `test Maybe Nothing exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Nothing
    --^...Maybe.elm
""")


    fun `test Maybe doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = withDefault
    --^unresolved
""")


    // RESULT MODULE


    fun `test Result module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = Result.withDefault x 42
       --^...Result.elm
""")


    fun `test Result type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Result String Int
                 --^...Result.elm
""")


    fun `test Result Ok exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Ok 42
   --^...Result.elm
""")


    fun `test Result Err exposed`() = stubOnlyResolve(
            """
--@ main.elm
f = Err "uh oh"
    --^...Result.elm
""")


    fun `test Result module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f x = toMaybe x 42
      --^unresolved
""")


    // STRING MODULE


    fun `test String module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = String.length x
       --^...String.elm
""")


    fun `test String type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = String
                 --^...String.elm
""")


    fun `test String module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = length "hello"
      --^unresolved
""")


    // CHAR MODULE


    fun `test Char module imported`() = stubOnlyResolve(
            """
--@ main.elm
f x = Char.isUpper x
      --^...Char.elm
""")


    fun `test Char type exposed`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Char
                 --^...Char.elm
""")


    fun `test Char module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = isUpper 'A'
      --^unresolved
""")


    // TUPLE MODULE


    fun `test Tuple module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Tuple.first (0, 0)
      --^...Tuple.elm
""")


    fun `test Tuple module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = pair 0 0
    --^unresolved
""")


    // DEBUG MODULE


    fun `test Debug module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Debug.toString
    --^...Debug.elm
""")


    fun `test Debug module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = toString
    --^unresolved
""")


    // PLATFORM MODULE


    fun `test Platform module imported`() = stubOnlyResolve(
            """
--@ main.elm
f = Platform.worker
    --^...Platform.elm
""")


    fun `test Platform module exposes Program type`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Program
                 --^...Platform.elm
""")


    fun `test Platform module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = worker
    --^unresolved
""")


    // PLATFORM.CMD MODULE


    fun `test Platform Cmd module imported using Cmd alias (module ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Cmd.none
    --^...Platform/Cmd.elm
""")


    fun `test Platform Cmd module imported using Cmd alias (value ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Cmd.none
        --^...Platform/Cmd.elm
""")


    fun `test Platform Cmd module exposes Cmd type`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Cmd
                 --^...Platform/Cmd.elm
""")


    fun `test Platform Cmd module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = batch
    --^unresolved
""")


    // PLATFORM.SUB MODULE


    fun `test Platform Sub module imported using Sub alias (module ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Sub.none
    --^...Platform/Sub.elm
""")


    fun `test Platform Sub module imported using Sub alias (value ref)`() = stubOnlyResolve(
            """
--@ main.elm
f = Sub.none
        --^...Platform/Sub.elm
""")


    fun `test Platform Sub module exposes Sub type`() = stubOnlyResolve(
            """
--@ main.elm
type alias Foo = Sub
                 --^...Platform/Sub.elm
""")


    fun `test Platform Sub module doesn't expose anything else`() = stubOnlyResolve(
            """
--@ main.elm
f = batch
    --^unresolved
""")


}