package org.elm.ide.inspections

// These tests don't have access to the standard imports, so you can't use them in type signatures,
// although literals still work.
class TypeInferenceInspectionTest : ElmInspectionsTestBase(ElmTypeInferenceInspection()) {
    fun `test too many arguments to value`() = checkByText("""
foo : ()
foo = ()

main = <error descr="This value is not a function, but it was given 1 argument.">foo 1</error>
""")

    fun `test too many arguments to function`() = checkByText("""
foo : () -> () -> ()
foo a b = a

main = <error descr="The function expects 2 arguments, but it got 3 instead.">foo () () ()</error>
""")

    fun `test too many arguments to operator`() = checkByText("""
add : () -> () -> ()
add a b = a
infix left 6 (+) = add
main = <error descr="The function expects 2 arguments, but it got 3 instead.">(+) () () ()</error>
""")

    fun `test too many parameters in value`() = checkByText("""
main: ()
main <error descr="The function expects 0 parameters, but it got 1 instead.">foo</error> = foo
""")

    fun `test too many parameters in function`() = checkByText("""
main: () -> ()
main <error descr="The function expects 1 parameter, but it got 2 instead.">foo bar</error> = (foo, bar)
""")

    fun `test mismatched int value type`() = checkByText("""
main : ()
main = <error descr="Type mismatch.Required: ()Found: Float">1.0</error>
""")

    fun `test matched int negation`() = checkByText("""
main = -1
""")

    fun `test matched float negation`() = checkByText("""
main = -1.0
""")

    fun `test mismatched negation`() = checkByText("""
main = -<error descr="Type mismatch.Required: numberFound: String">""</error>
""")

    fun `test mismatched tuple value type from missing field`() = checkByText("""
main : ((), (), ())
main = <error descr="Type mismatch.Required: ((), (), ())Found: ((), ())">((), ())</error>
""")

    fun `test mismatched tuple value type from wrong field type`() = checkByText("""
main : ((), ())
main = <error descr="Type mismatch.Required: ((), ())Found: (Float, ())">(1.0, ())</error>
""")

    fun `test mismatched tuple value type from extra field`() = checkByText("""
main : ((), ())
main = <error descr="Type mismatch.Required: ((), ())Found: ((), (), ())">((), (), ())</error>
""")

    fun `test matched tuple value type`() = checkByText("""
main : ((), ())
main = ((), ())
""")

    fun `test mismatched return type from argument`() = checkByText("""
type Foo = Bar
main : Foo -> ()
main a = <error descr="Type mismatch.Required: ()Found: Foo">a</error>
""")

    fun `test mismatched return type from float literal`() = checkByText("""
type Foo = Bar
main : () -> Foo
main a = <error descr="Type mismatch.Required: FooFound: Float">1.0</error>
""")

    fun `test correct value type from record`() = checkByText("""
main : {x: (), y: ()}
main = {x = (), y = ()}
""")

    fun `test correct value type from record alias`() = checkByText("""
type alias A = {x: (), y: ()}
main : A
main = {x = (), y = ()}
""")

    fun `test correct value type from called record constructor`() = checkByText("""
type alias A = {x: (), y: ()}
main : A
main = A () ()
""")

    fun `test calling record without constructor`() = checkByText("""
main : {x: (), y: ()} -> {x: (), y: ()}
main a = <error descr="This value is not a function, but it was given 2 arguments.">a () ()</error>
""")

    fun `test correct value type from record constructor as function`() = checkByText("""
type alias A = {x: (), y: ()}
main : () -> (() -> () -> A)
main _ = A
""")

    fun `test correct value from field accessor`() = checkByText("""
main : ()
main = .x {x=()}
""")

    fun `test value from parameter with base record identifier`() = checkByText("""
type alias R = {x: (), y: ()}
main : R -> R
main r = { r | x = () }
""")

    fun `test mismatched value from parameter with base record identifier`() = checkByText("""
type alias R = {x: (), y: ()}
main : R -> ()
main r = <error descr="Type mismatch.Required: ()Found: R">{ r | x = () }</error>
""")

    fun `test mismatched base record identifier`() = checkByText("""
main : () -> ()
main r = { <error descr="Type must be a record.Found: ()">r</error> | x = () }
""")

    fun `test unknown field from parameter with base record identifier`() = checkByText("""
type alias R = {x: (), y: ()}
main : R -> R
main r = { r | x = (), <error descr="Record does not have field 'z'">z</error> = () }
""")

    fun `test mismatched field from parameter with base record identifier`() = checkByText("""
type alias R = {x: (), y: ()}
main : R -> R
main r = { r | <error descr="Type mismatch.Required: ()Found: String">x</error> = "" }
""")
    fun `test matched parameter with base record identifier`() = checkByText("""
type alias R = {x: (), y: ()}
foo : { r | x : () } -> ()
foo r = r.x

main : R -> ()
main r = foo r
""")

    fun `test mismatched value from argument with base record identifier`() = checkByText("""
type Foo = Bar
type alias R = {x: (), y: ()}
foo : { r | x : ()} -> ()
foo r = r.x

main : R -> Foo
main r = <error descr="Type mismatch.Required: FooFound: ()">foo r</error>
""")

    fun `test mismatched parameter with base record identifier`() = checkByText("""
type Foo = Bar
type alias R = {x: (), y: ()}
foo : { r | x : ()} -> { r | x : ()}
foo r = r

main : R
main = foo <error descr="Type mismatch.Required: { r | x: () }Found: { y: () }">{ y = () }</error>
""")

    fun `test field accessor as argument`() = checkByText("""
type alias R = {x: (), y: ()}
foo : (R -> ()) -> ()
foo _ = ()

main : ()
main = foo .x
""")

    fun `test correct value type from parametric record alias`() = checkByText("""
type alias A a = {x: a, y: ()}
main : A ()
main = {x = 1.0, y = ()}
""")

    fun `test mismatched value type from record subset`() = checkByText("""
type alias R = {x: (), y: ()}
main : R
main = <error descr="Type mismatch.Required: RFound: { x: () }">{x = ()}</error>
""")

    fun `test mismatched value type from record superset`() = checkByText("""
type alias R = {x: (), y: ()}
main : R
main = <error descr="Type mismatch.Required: RFound: { x: (), y: (), z: () }">{x = (), y=(), z=()}</error>
""")

    fun `test matched field accessor chains`() = checkByText("""
type alias A = {x: ()}
type alias B = {a: A}
type alias C = {b: B}
fieldAccessor : C -> ()
fieldAccessor c = c.b.a.x

exprAccessor : ()
exprAccessor = (C (B (A ()))).b.a.x

recordAccessor : ()
recordAccessor = {b = {a = { x = () } } }.b.a.x
""")

    fun `test mismatch in final part of field accessor chains`() = checkByText("""
type Foo = Bar
type alias A = {x: ()}
type alias B = {a: A}
type alias C = {b: B}
fieldAccessor : C -> Foo
fieldAccessor c = <error descr="Type mismatch.Required: FooFound: ()">c.b.a.x</error>

exprAccessor : Foo
exprAccessor = <error descr="Type mismatch.Required: FooFound: ()">(C (B (A ()))).b.a.x</error>

recordAccessor : Foo
recordAccessor = <error descr="Type mismatch.Required: FooFound: ()">{b = {a = { x = () } } }.b.a.x</error>
""")

    fun `test non-record in middle of accessor chain`() = checkByText("""
type Foo = Bar
type alias A = {x: ()}
type alias B = {a: A}
type alias C = {b: B}
fieldAccessor : C -> Foo
fieldAccessor c = c.b.a.<error descr="Type mismatch.Required: recordFound: ()">x</error>.z.z

exprAccessor : ()
exprAccessor = (C (B (A ()))).b.a.<error descr="Type mismatch.Required: recordFound: ()">x</error>.z.z

recordAccessor : ()
recordAccessor = {b = {a = { x = () } } }.b.a.<error descr="Type mismatch.Required: recordFound: ()">x</error>.z.z
""")

    fun `test missing field in accessor chains`() = checkByText("""
type alias A = {x: ()}
type alias B = {a: A}
type alias C = {b: B}
fieldAccessor : C -> ()
fieldAccessor c = c.b.<error descr="Record does not have field 'z'">z</error>.x

exprAccessor : ()
exprAccessor = (C (B (A ()))).b.<error descr="Record does not have field 'z'">z</error>.x

recordAccessor : ()
recordAccessor = {b = {a = { x = () } } }.b.<error descr="Record does not have field 'z'">z</error>.x
""")

    fun `test matched value type from union case`() = checkByText("""
type Maybe a = Just a | Nothing
main : Maybe a
main = Nothing
""")

    fun `test mismatched value type from union case`() = checkByText("""
type Maybe a = Just a | Nothing
type Foo = Bar
main : Maybe a
main = <error descr="Type mismatch.Required: Maybe aFound: Foo">Bar</error>
""")

    fun `test invalid constructor as type annotation`() = checkByText("""
type Maybe a = Just a | Nothing
main : <error descr="Unresolved reference 'Just'">Just a</error> -> ()
main a = ()
""")

    fun `test matched value from union constructor`() = checkByText("""
type Maybe = Just () | Nothing
foo : (() -> Maybe) -> () -> ()
foo _ _ = ()

main = foo Just ()
""")

    fun `test matched value from function call`() = checkByText("""
foo : a -> ()
foo _ = ()

main : ()
main = foo 1
""")

    fun `test matched value from function call with parenthesized arguments`() = checkByText("""
foo : a -> a -> ()
foo _ _ = ()

main : Int
main = foo (()) (())
""")

    fun `test mismatched value from function call`() = checkByText("""
type Foo = Bar
foo : a -> ()
foo _ = ()

main : Foo
main = <error descr="Type mismatch.Required: FooFound: ()">foo 1</error>
""")

    fun `test mismatched value from port call`() = checkByText("""
port module Main exposing (foo)
port foo : a -> ()
type Foo = Bar

main : Foo
main = <error descr="Type mismatch.Required: FooFound: ()">foo 1</error>
""")

    fun `test matched function call from parameter`() = checkByText("""
type Foo = Bar
main : (() -> Foo) -> Foo
main fn = fn ()
""")

    fun `test mismatched function call from parameter`() = checkByText("""
type Foo = Bar
main : (() -> Foo) -> ()
main fn = <error descr="Type mismatch.Required: ()Found: Foo">fn ()</error>
""")

    //  This tests that the type refs in function calls are resolved to the correct module
    fun `test mismatched function call with conflicting type name`() = checkByFileTree("""
--@ main.elm
import People.Washington exposing (person)
import People.Costanza exposing (People(..))
main = person <error descr="Type mismatch.Required: People.Washington.PeopleFound: People.Costanza.People">George</error>
--^

--@ People/Washington.elm
module People.Washington exposing (People(..), person)
type People = George

person : People -> ()
person _ = ()

--@ People/Costanza.elm
module People.Costanza exposing (People(..))
type People = George
""")

    //  This tests that the type refs in annotations are resolved to the correct module
    fun `test matched value annotation with concrete union type from other module`() = checkByFileTree("""
--@ main.elm
import People.Washington exposing (People(..), person)

main : Maybe People
main = person George
--^

--@ Foo.elm
module People.Washington exposing (People(..), person)
import Maybe exposing (Maybe(..))

type People = George

person : People -> Maybe People
person a = Just a

--@ Maybe.elm
module Maybe exposing (Maybe(..))

type Maybe a
    = Just a
    | Nothing
""")

    fun `test duplicate function parameter`() = checkByText("""
main a <error descr="Conflicting name declaration">a</error> = ()
""")

    fun `test function parameter duplicating function name`() = checkByText("""
main <error descr="Conflicting name declaration">main</error> = ()
""")

    fun `test parameter name duplicating top level`() = checkByText("""
foo = ()
main <error descr="Conflicting name declaration">foo</error> = ()
""")

    fun `test duplicate name in anonymous function`() = checkByText("""
main a = (\<error descr="Conflicting name declaration">a</error> -> a)
""")

    fun `test if-else with mismatched condition`() = checkByText("""
main = if <error descr="Type mismatch.Required: BoolFound: Float">1.0</error> then 1 else 2
""")

    fun `test if-else with mismatched else`() = checkByText("""
main = if True then 1.0 else <error descr="Type mismatch.Required: FloatFound: String">"foo"</error>
""")

    fun `test value with mismatched if-else `() = checkByText("""
main : ()
main = <error descr="Type mismatch.Required: ()Found: Float">if True then 1.0 else 2.0</error>
""")

    fun `test if-else with mismatched branches`() = checkByText("""
main = if True then 1.0 else if True then <error descr="Type mismatch.Required: FloatFound: String">"foo"</error> else ()
""")

    fun `test mismatched elements`() = checkByText("""
main = ["", <error descr="Type mismatch.Required: StringFound: Float">1.0</error>, ()]
""")

    fun `test matched lambda type with closure`() = checkByText("""
type Foo = Bar
main : Foo -> (Foo -> Foo)
main a = (\_ -> a)
""")

    fun `test returning lambda`() = checkByText("""
main : () -> ()
main = (\a -> a)
""")

    fun `test record update in lambda`() = checkByText("""
type alias R = {x : ()}
main : R -> R
main = (\r -> { r | x = () })
""")

    fun `test matched lambda type with closure pattern matching` () = checkByText("""
type Foo = Bar
main : (Foo, Foo) -> ((Foo, Foo) -> Foo)
main a = (\(_, b) -> b)
""")

    fun `test mismatched lambda type with closure`() = checkByText("""
type Foo = Bar
main : () -> (Foo -> Foo)
main a = <error descr="Type mismatch.Required: Foo → FooFound: a → ()">(\_ -> a)</error>
""")

    fun `test mismatched tuple pattern in parameter`() = checkByText("""
type Foo = Foo () ()
main : (Foo, Foo) -> ()
main (a, b) = <error descr="Type mismatch.Required: ()Found: Foo">a</error>
""")

    fun `test matched union pattern in parameter`() = checkByText("""
type Foo = Foo ()
main : Foo -> ()
main (Foo foo) = foo
""")

    fun `test mismatched union pattern in parameter`() = checkByText("""
type Foo = Foo ()
main : Foo -> Foo
main (Foo foo) = <error descr="Type mismatch.Required: FooFound: ()">foo</error>
""")

    fun `test union pattern in parameter with too many args`() = checkByText("""
type Foo = Foo ()
main : Foo -> ()
main (<error descr="The function expects 1 argument, but it got 2 instead.">Foo foo bar</error>) = foo
""")

    fun `test union pattern in parameter with too few args`() = checkByText("""
type Foo = Foo () ()
main : Foo -> ()
main (<error descr="The function expects 2 arguments, but it got 1 instead.">Foo foo</error>) = foo
""")

    fun `test union pattern in parameter with too many args for non-constructor`() = checkByText("""
type Foo = Foo
main : Foo -> ()
main (<error descr="This value is not a function, but it was given 1 argument.">Foo foo</error>) = foo
""")

    fun `test let-in with mismatched type in annotated inner func`() = checkByText("""
main : ()
main =
    let
        foo : ()
        foo = <error descr="Type mismatch.Required: ()Found: String">""</error>
    in
        foo
""")

    fun `test let-in with mismatched type from annotated inner func`() = checkByText("""
type Foo = Bar
main : Foo
main =
    let
        foo : ()
        foo = ()
    in
        <error descr="Type mismatch.Required: FooFound: ()">foo</error>
""")

    fun `test let-in function without annotation and one parameter`() = checkByText("""
main : ()
main =
    let
        foo a = ()
    in
        foo 1
""")

    fun `test let-in function without annotation and two parameters`() = checkByText("""
main : ()
main =
    let
        foo a b = ()
    in
        foo 1 2
""")

    fun `test mismatched return value from let-in record binding`() = checkByText("""
main : ()
main =
    let
        {x, y} = {x = 1, y = ""}
    in
        <error descr="Type mismatch.Required: ()Found: String">y</error>
""")

    fun `test mismatched return value from let-in tuple binding`() = checkByText("""
main : ()
main =
    let
        (x, y) = (1, "")
    in
        <error descr="Type mismatch.Required: ()Found: String">y</error>
""")

    fun `test matched record argument to let-in function`() = checkByText("""
main : ()
main =
    let
        foo {x, y} = x
    in
        foo {x=(), y=()}
""")

    fun `test cyclic definition in let-in record binding`() = checkByText("""
main : ()
main =
    let
        {x, y} = {x = 1, y = <error descr="Value cannot be defined in terms of itself">y</error>}
    in
        y
""")

    fun `test invalid let-in record binding`() = checkByText("""
main : ()
main =
    let
        <error descr="Type mismatch.Required: ()Found: { x: a, y: b }">{x, y}</error> = ()
    in
        y
""")

    fun `test invalid let-in tuple binding`() = checkByText("""
main : ()
main =
    let
        <error descr="Type mismatch.Required: ()Found: (a, b)">(x, y)</error> = ()
    in
        y
""")

    fun `test mismatch in chained let-in tuple binding`() = checkByText("""
main : ()
main =
    let
        (x, y) = ((), "")
        (z, w) = (x, y)
    in
        <error descr="Type mismatch.Required: ()Found: String">w</error>
""")


    fun `test returning function`() = checkByText("""
main : () -> ()
main =
    let
        foo a = a
    in
        foo
""")

    fun `test returning partially applied function`() = checkByText("""
main : () -> ()
main =
    let
        foo a b = b
    in
        foo ()
""")

    fun `test matched function type alias in annotation`() = checkByText("""
type Foo = Bar
type alias F = Foo -> ()

foo : F -> F
foo a b = a b
""")

    fun `test mismatched function type alias in annotation`() = checkByText("""
type Foo = Bar
type alias F = Foo -> ()

foo : F -> F
foo a b = <error descr="Type mismatch.Required: ()Found: Foo">b</error>
""")

    fun `test partial pattern in function parameter from cons`() = checkByText("""
main (<error descr="Pattern does not cover all possibilities">x :: []</error>) = ()
""")

    fun `test partial pattern in function parameter from list`() = checkByText("""
main (<error descr="Pattern does not cover all possibilities">[x]</error>) = ()
""")

    fun `test partial pattern in function parameter from constant`() = checkByText("""
main (<error descr="Pattern does not cover all possibilities">""</error>) = ()
""")

    fun `test partial pattern in lambda parameter from constant`() = checkByText("""
main = (\<error descr="Pattern does not cover all possibilities">""</error> -> "")
""")

    fun `test bad self-recursion in annotated value`() = checkByText("""
main : ()
<error descr="Infinite recursion">main = main</error>
""")

    fun `test bad self-recursion in unannotated value`() = checkByText("""
<error descr="Infinite recursion">main = main</error>
""")

    fun `test allowed self-recursion in annotated function`() = checkByText("""
main : () -> ()
main a = main a -- This is a runtime error, not compile time
""")

    fun `test allowed self-recursion in unannotated function`() = checkByText("""
main a = main a -- This is a runtime error, not compile time
""")

    fun `test allowed self-recursion in lambda`() = checkByText("""
main : ()
main = (\_ -> main) 1
""")

    fun `test bad mutual recursion`() = checkByText("""
<error descr="Infinite recursion">foo = bar</error>
bar = foo
""")

    fun `test uncurrying return value from unannotated function`() = checkByText("""
lazy3 : (a -> b -> c -> ()) -> a -> b -> c -> ()
lazy3 a = a

apply1 fn a = embed (fn a)

embed : () -> () -> ()
embed a b = a

main : (a -> ()) -> a -> a -> ()
main fn a = lazy3 apply1 fn a
""")

    fun `test function argument mismatch in case expression`() = checkByText("""
foo : () -> ()
foo a = a

main =
    case foo <error descr="Type mismatch.Required: ()Found: String">""</error> of
        _ -> ()
""")

    fun `test function argument mismatch in case branch`() = checkByText("""
foo : () -> ()
foo a = a

main =
    case () of
        _ -> foo <error descr="Type mismatch.Required: ()Found: String">""</error>
""")

    fun `test value mismatch from case`() = checkByText("""
main : ()
main =
    <error descr="Type mismatch.Required: ()Found: String">case () of
        _ -> ""</error>
""")

    fun `test case branches with mismatched types`() = checkByText("""
main : ()
main =
    case () of
        "" -> ()
        "x" -> <error descr="Type mismatch.Required: ()Found: String">""</error>
        _ -> ()
""")

    // issue #113
    fun `test case branches with union value call`() = checkByText("""
type Maybe a = Just a | Nothing

foo : Maybe (List a)
foo = Nothing

main =
    case foo of
       Just [] -> ()
       _ -> ()
""")

    fun `test case branches with mismatched tuple type`() = checkByText("""
type Foo = Bar
type Baz = Qux(Foo, Foo)

main : Baz -> ()
main x =
    <error descr="Type mismatch.Required: ()Found: Foo">case x of
        Qux (y, z) -> z</error>
""")

    fun `test case branches using union patterns with constructor argument`() = checkByText("""
type Maybe a
    = Just a
    | Nothing

type Foo
    = Bar ()
    | Baz ()
    | Qux (Maybe ()) ()

main : Foo -> ()
main arg =
    case arg of
        Bar x -> ()
        Baz x -> x
        Qux Nothing x -> x
""")

    fun `test case branches using union patterns with tuple destructuring of var`() = checkByText("""
type Maybe a
    = Just a
    | Nothing

type Foo
    = Bar (Maybe ((), ()))
    | Baz ()

main : Foo -> ()
main arg =
    case arg of
        Bar (Just (x, y)) -> y
        Baz x -> x
        _ -> ()
""")

    fun `test case branches using union patterns with tuple destructuring of record`() = checkByText("""
type Maybe a
    = Just a
    | Nothing

type Foo
    = Bar (Maybe {x: ()})
    | Baz ()

main : Foo -> ()
main arg =
    case arg of
        Bar (Just {x}) -> x
        Baz x -> x
        _ -> ()
""")

    fun `test case branches using union patterns to unresolved type`() = checkByText("""
-- This will lead to unresolved reference errors, but we still to test that we're binding
-- the parameters so that we can infer the branch expressions.
main arg =
    case arg of
        Bar (<error descr="Unresolved reference 'Just'">Just {x}</error>) -> x
        <error descr="Unresolved reference 'Baz'">Baz x</error> -> x
        _ -> ()

""")

    fun `test function parameters using union patterns to unresolved type`() = checkByText("""
<error descr="<module declaration> expected, got 'main'">main</error> <error descr="Unresolved reference 'Foo'">Foo bar</error> = <error descr="Value cannot be defined in terms of itself">bar</error>
""")

    fun `test valid case branch with cons pattern`() = checkByText("""
main : ()
main =
    case [()] of
        x :: xs -> x
        _ -> ()
""")

    fun `test valid case branch with list pattern`() = checkByText("""
main : ()
main =
    case [()] of
        [x, y] -> x
        _ -> ()
""")

    fun `test valid case branch with const and list pattern`() = checkByText("""
main : ()
main =
    case [()] of
        z :: [x, y] -> x
        _ -> ()
""")

    fun `test invalid return value from cons pattern`() = checkByText("""
main : ()
main =
    <error descr="Type mismatch.Required: ()Found: String">case [""] of
        x :: xs -> x</error>
""")

    // TODO [drop 0.18] remove this test
    fun `test 0_18 top-level pattern declarations`() = checkByText("""
(a, b) = (1, ())
{x} = {x = ()}
foo : ()
foo = b

bar : ()
bar = x
""")

    fun `test nested forward references`() = checkByText("""
main : () -> ()
main m =
  let
    x a = y a
    y a = z r m
    z a b = a
    (q, r) = (m, ())
  in
  x ()
""")

    fun `test mismatched left operand to non-associative operator`() = checkByText("""
foo : () -> () -> ()
foo a b = a
infix non 4 (~~) = foo

main a = <error descr="Type mismatch.Required: ()Found: String">""</error> ~~ ()
""")

    fun `test mismatched right operand to non-associative operator`() = checkByText("""
foo : () -> () -> ()
foo a b = a
infix non 4 (~~) = foo

main a = () ~~ <error descr="Type mismatch.Required: ()Found: String">""</error>
""")

    fun `test chained non-associative operator`() = checkByText("""
foo : () -> () -> ()
foo a b = a
infix non 4 (~~) = foo

main a = <error descr="Operator (~~) is not associative, and so cannot be chained">() ~~ () ~~ ()</error>
""")

    fun `test matched left associative chain`() = checkByText("""
type Foo = Bar
foo : Foo -> () -> Foo
foo a b = a
infix left 4 (~~) = foo

main : Foo
main = Bar ~~ () ~~ ()
""")

    fun `test matched right associative chain`() = checkByText("""
type Foo = Bar
foo : () -> Foo -> Foo
foo a b = b
infix right 4 (~~) = foo

main : Foo
main = () ~~ () ~~ Bar
""")

    fun `test mismatched left associative chain`() = checkByText("""
type Foo = Bar
foo : () -> () -> Foo
foo a b = Bar
infix left 4 (~~) = foo

main a = <error descr="Type mismatch.Required: ()Found: Foo">() ~~ ()</error> ~~ ()
""")

    fun `test mismatched right associative chain`() = checkByText("""
type Foo = Bar
foo : () -> () -> Foo
foo a b = Bar
infix right 4 (~~) = foo

main a = () ~~ <error descr="Type mismatch.Required: ()Found: Foo">() ~~ ()</error>
""")

    fun `test apply-right into Maybe`() = checkByText("""
type Maybe a = Just a | Nothing
apR : a -> (a -> b) -> b
apR x f = f x
infix left  0 (|>) = apR

main : Maybe ()
main = () |> Just
""")

    fun `test multiple non-associative operators`() = checkByText("""
type Bool = True | False
lt : a -> a -> Bool
lt a b = True
and : Bool -> Bool -> Bool
and a b = False

infix right 3 (&&) = and
infix non   4 (<)  = lt

main : Bool
main = 1 < 2 && 3 < 4
""")
}
