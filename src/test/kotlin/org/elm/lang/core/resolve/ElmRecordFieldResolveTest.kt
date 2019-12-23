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

    fun `test simple field accessor function`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r =
    .field r
      --^
""")

    fun `test field access on return value inside unannotated function`() = checkByCode(
            """
type alias R = { field : () }
                 --X
r : () -> R
r unit = { field = unit }

main = (r ()).field
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

    fun `test field access to field in record parameter`() = checkByCode(
            """
type alias R a = { a | field : () }
type alias S = { s : R { field2 : () } }
                          --X
main : S -> ()
main r = r.s.field2
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

    fun `test field access in lambda call`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r = (\rr -> rr.field) r
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

    fun `test field access of variant param`() = checkByCode(
            """
type T = T { field : () }
              --X
main : T -> ()
main t =
     case t of
         T record ->
             record.field
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

    fun `test record value returned from function`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R
main = { field = () }
          --^
""")

    fun `test record value returned from lambda`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R
main = (\_ -> { field = () }) 1
                 --^
""")

    fun `test nested decl field access`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r = 
  let
    nest = r.field
             --^
  in
  nest
""")

    fun `test nested decl mapper`() = checkByCode(
            """                                        
type alias R = { field : () }                          
                 --X                                   
type Box a = Box a                                     
                                                       
map : (a -> b) -> Box a -> Box b                       
map f (Box a) = Box (f a)                              
                                                       
main : Box R -> Box R                                  
main box =                                             
    let                                                
        f r = { r | field = () }                       
                     --^                               
    in                                                 
    map f box                                          
""")

    fun `test multi resolve`() = checkMultiResolve(
            """
type alias R = { field : () }
type alias S = { field : () }
first : () -> () -> ()
first a _ = a
main : R -> S -> ()
main r s =
  let
    nest t = t.field
               --^                               
  in
  first (nest r) (nest s)        
    """)

    fun `test ref to destructuring in function parameter`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main { field } = field
       --^
""")

    fun `test value ref through destructuring in function parameter`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main { field } = field
                 --^
""")

    fun `test ref through destructuring in case`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r = 
  case r of
      { field } -> field
                    --^
""")

    fun `test ref to destructuring in case`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : R -> ()
main r = 
  case r of
      { field } -> field
        --^
""")

    fun `test repeated reference in list 1`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : List R
main = 
    [ { field = () }
        --^                               
    ]                           
""")

    fun `test repeated reference in list 2`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : List R
main = 
    [ { field = () }
    , { field = () }
        --^                               
    ]
""")

    fun `test repeated reference in list 3`() = checkByCode(
            """
type alias R = { field : () }
                 --X
main : List R
main = 
    [ { field = () }
    , { field = () }
    , { field = () }
        --^                               
    ]
""")

    fun `test nested extension aliases with funcion in type variable passed through another variable via forward pipeline`() = checkByCode(
            """
infix left  0 (|>) = apR
apR : a -> (a -> b) -> b
apR x f = f x

type alias R = { field : () }
                  --X
type alias Outer r = { r : Type (r -> r) }
type Type a = Type a

foo : Outer r -> Outer r
foo r = r

main : Outer R
main =
    { r = Type (\r -> { r | field = () }) } |> foo 
                             --^                               
""")
}
