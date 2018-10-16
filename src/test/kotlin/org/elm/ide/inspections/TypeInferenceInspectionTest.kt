package org.elm.ide.inspections

// These tests don't have access to the standard imports, so you can't use them in type signatures,
// although literals still work.
class TypeInferenceInspectionTest : ElmInspectionsTestBase(TypeInferenceInspection()) {
    fun `test too many arguments to value`() = checkByText("""
foo = ()

main = <error descr="This value is not a function, but it was given 1 argument.">foo 1</error>
""")

    fun `test too many arguments to function`() = checkByText("""
foo a b = a

main = <error descr="The function expects 2 arguments, but it got 3 instead.">foo 1 2 3</error>
""")

    fun `test too many arguments to operator`() = checkByText("""
add a b = a
infix left 6 (+) = add
main = <error descr="The function expects 2 arguments, but it got 3 instead.">(+) 1 2 3</error>
""")

    fun `test mismatched int value type`() = checkByText("""
main : ()
main = <error descr="Type mismatch.Required: ()Found: Float">1.0</error>
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

    fun `test correct value type from parametric record alias`() = checkByText("""
type alias A a = {x: a, y: ()}
main : A ()
main = {x = 1.0, y = ()}
""")

    fun `test mismatched value type from record subset`() = checkByText("""
type alias R = {x: (), y: ()}
main : R
main = <error descr="Type mismatch.Required: {x: (),y: ()}Found: {x: ()}">{x = ()}</error>
""")

    fun `test mismatched value type from record superset`() = checkByText("""
type alias R = {x: (), y: ()}
main : R
main = <error descr="Type mismatch.Required: {x: (),y: ()}Found: {x: (),y: (),z: ()}">{x = (), y=(), z=()}</error>
""")

    fun `test matched value type union case`() = checkByText("""
type Maybe a = Just a | Nothing
main : Maybe a
main = Nothing
""")

    fun `test mismatched value type union case`() = checkByText("""
type Maybe a = Just a | Nothing
type Foo = Bar
main : Maybe a
main = <error descr="Type mismatch.Required: Maybe aFound: Foo">Bar</error>
""")

    fun `test union type annotation with too few parameters`() = checkByText("""
type Maybe a = Just a | Nothing
main : <error descr="The type expects 1 argument, but it got 0 instead.">Maybe</error> -> ()
main a = ()
""")

    fun `test union type annotation with too many parameters`() = checkByText("""
type Maybe a = Just a | Nothing
main : <error descr="The type expects 1 argument, but it got 2 instead.">Maybe () ()</error> -> ()
main a = ()
""")

    fun `test invalid constructor as type annotation`() = checkByText("""
type Maybe a = Just a | Nothing
main : <error descr="Unresolved reference 'Just'">Just a</error> -> ()
main a = ()
""")

    fun `test matched value from union constructor`() = checkByText("""
type Maybe = Just () | Nothing
foo : (() -> Maybe) -> () -> ()
foo _ = ()

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
foo _ = ()

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

    fun `test mismatched lambda type with closure`() = checkByText("""
type Foo = Bar
main : () -> (Foo -> Foo)
main a = <error descr="Type mismatch.Required: Foo -> FooFound: unknown -> ()">(\_ -> a)</error>
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

    fun `test let-in function without annotation`() = checkByText("""
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
        <error descr="Type mismatch.Required: ()Found: {x: a,y: b}">{x, y}</error> = ()
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

    fun `test bad self-recursion`() = checkByText("""
main : ()
<error descr="Infinite recursion">main = main</error>
""")

    fun `test bad mutual recursion`() = checkByText("""
<error descr="Infinite recursion">foo = bar</error>

<error descr="Infinite recursion">bar = foo</error>
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
main : () -> ()
main =
    case () of
        "" -> ()
        "x" -> <error descr="Type mismatch.Required: ()Found: String">""</error>
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
}
