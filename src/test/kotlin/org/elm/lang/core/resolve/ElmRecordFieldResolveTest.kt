package org.elm.lang.core.resolve


class ElmRecordFieldResolveTest : ElmResolveTestBase() {
    fun `test simple field access`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r = r.field
           --^
""")

    fun `test chained field access at end of chain`() = checkByCode(
            """
type alias S = { nested : () }
                 --X
type alias R = { field : S }
main : R -> ()
main r = r.field.nested
                  --^
""")

    fun `test chained field access at middle of chain`() = checkByCode(
            """
type alias S = { nested : () }
type alias R = { field : S }
                 --X
main : R -> ()
main r = r.field.nested
           --^
""")

    fun `test field access to parameterized record`() = checkByCode(
            """
type alias R a = { field : a }
                 --X
main : R () -> ()
main r = r.field
           --^
""")

    fun `test field access to nested parameterized record`() = checkByCode(
            """
type alias S = { nested : () }
                 --X
type alias R a = { field : a }
main : R S -> ()
main r = r.field.nested
                  --^
""")

    fun `test record update`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> R
main r = { r | field = ()}
                --^
""")

    fun `test record update access`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r = { r | field = () }.field
                           --^
""")

    fun `test record value in function call`() = checkByCode(
            """
type alias R = { field : () }
                 --X
func : R -> ()
func _ = ()

main : ()
main = func { field = () }
               --^
""")

    fun `test record value in forward pipeline`() = checkByCode(
            """
infix left  0 (|>) = apR
apR : a -> (a -> b) -> b
apR x f = f x

type alias R = { field : () }
                 --X
func : R -> ()
func _ = ()

main : ()
main = { field = () } |> func
          --^
""")

    fun `test record value in backward pipeline`() = checkByCode(
            """
infix right 0 (<|) = apL
apL : (a -> b) -> a -> b
apL f x =
  f x

type alias R = { field : () }
                 --X
func : R -> ()
func _ = ()

main : ()
main = func <| { field = () }
                 --^
""")
}
