package org.elm.ide.inspections

class TypeInferenceInspectionTest : ElmInspectionsTestBase(ElmTypeInferenceInspection()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

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
main : Int
main = -1
""")

    fun `test matched float negation`() = checkByText("""
main : Float
main = -1.0
""")

    fun `test mismatched negation`() = checkByText("""
main = -<error descr="Type mismatch.Required: numberFound: String">""</error>
""")

    fun `test negate from pattern match`() = checkByText("""
type alias N = Float
type Foo = Bar N

main : Foo -> ()
main foo =
    <error descr="Type mismatch.Required: ()Found: N">case foo of
        Bar n -> -n</error>
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

    fun `test mismatched return type from List argument`() = checkByText("""
main : List Int -> List ()
main a = <error descr="Type mismatch.Required: List ()Found: List Int">a</error>
""")

    fun `test mismatched return type from shadowed List`() = checkByText("""
type List a = List a
main : List ()
main = <error descr="Type mismatch.Required: List ()Found: List.List a">[]</error>
""")

    fun `test mismatched return type from float literal`() = checkByText("""
type Foo = Bar
main : () -> Foo
main a = <error descr="Type mismatch.Required: FooFound: Float">1.0</error>
""")

    fun `test mismatched value type from function without annotation`() = checkByText("""
foo = ()
main : String
main = <error descr="Type mismatch.Required: StringFound: ()">foo</error>
""")

    fun `test calling function without annotation with unconstrained parameters`() = checkByText("""
foo a = a
main : ()
main = <error descr="Type mismatch.Required: ()Found: String">foo ""</error>
""")

    fun `test parameterized function type`() = checkByText("""
type Foo a = Bar
main : (Foo ty -> Foo ty) -> ()
main a =
    <error descr="Type mismatch.Required: ()Found: Foo ty → Foo ty">a</error>
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

    fun `test record constructor with no fields`() = checkByText("""
type alias R = {}
foo : ()
foo = <error descr="Type mismatch.Required: ()Found: R">R</error>
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
type alias R = {x: (), y: ()}
foo : { r | x : ()} -> { r | x : ()}
foo r = r

main : R
main = foo <error descr="Type mismatch.Required: { r | x : () }Found: { y : () }">{ y = () }</error>
""")


    fun `test extension record argument with more fields than extension parameter`() = checkByText("""
type alias Large a = { a | field1 : String, field2 : String }
type alias Small a = { a | field1 : String }

foo : Small a -> String
foo _ = ""

main : Large a -> String
main arg = foo arg
""")

    fun `test extension record argument in function type with more fields than extension parameter`() = checkByText("""
type alias Large a = { a | field1 : String, field2 : String }
type alias Small a = { a | field1 : String }

foo : Small a -> String
foo _ = ""

main : Large a -> String
main = foo
""")

    fun `test modifying extended record through extension alias`() = checkByText("""
type alias Small a = { a | field1 : String }
type alias Large = Small { field2 : String }

foo : Small a -> Small a
foo a = a

main : Large -> ()
main r =
    let
        rr = foo r
    in
    <error descr="Type mismatch.Required: ()Found: Small Large">{ rr | field2 = "" }</error>
""")

    fun `test alias to extension record`() = checkByText("""
type alias Record a = { a | field : () }
type alias Alias a = Record a

foo : Alias a  -> Alias a
foo it = it

main : Alias a  -> ()
main model =
    <error descr="Type mismatch.Required: ()Found: Alias a">foo model</error>
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
main : A Float
main = {x = 1.0, y = ()}
""")

    fun `test mismatched value from chained alias`() = checkByText("""
type Foo a = Foo
type alias Bar = Foo ()

main : Bar -> Foo Int
main a = <error descr="Type mismatch.Required: Foo IntFound: Bar">a</error>
""")

    fun `test mismatched value type from parametric record alias`() = checkByText("""
type alias A a = {x: a, y: ()}
main : A ()
main = <error descr="Type mismatch.Required: A ()Found: { x : Float, y : () }">{x = 1.0, y = ()}</error>
""")

    fun `test mismatched value type from record subset`() = checkByText("""
type alias R = {x: (), y: ()}
main : R
main = <error descr="Type mismatch.Required: RFound: { x : () }">{x = ()}</error>
""")

    fun `test mismatched value type from record superset`() = checkByText("""
type alias R = {x: (), y: ()}
main : R
main = <error descr="Type mismatch.Required: RFound: { x : (), y : (), z : () }">{x = (), y=(), z=()}</error>
""")

    fun `test mismatched return type from propagated type vars`() = checkByText("""
type alias A a = {x: Maybe a}
type alias B a = A a
main : B () -> Maybe Int
main b = <error descr="Type mismatch.Required: Maybe IntFound: Maybe ()">b.x</error>
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
main : Maybe a
main = Nothing
""")

    fun `test mismatched value type from union case`() = checkByText("""
type Foo = Bar
main : Maybe a
main = <error descr="Type mismatch.Required: Maybe aFound: Foo">Bar</error>
""")

    fun `test invalid constructor as type annotation`() = checkByText("""
main : <error descr="Unresolved reference 'Just'">Just</error> a -> ()
main a = ()
""")

    fun `test matched value from union constructor`() = checkByText("""
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
foo : Int -> Int -> Int
foo a b = a

main : Int
main = foo (1) (2)
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

--@ People/Washington.elm
module People.Washington exposing (People(..), person)

type People = George

person : People -> Maybe People
person a = Just a
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

    fun `test function with arguments returning lambda`() = checkByText("""
foo a = \b -> b

main : ()
main = <error descr="Type mismatch.Required: ()Found: String">foo "" ""</error>
""")

    fun `test lambda returning lambda`() = checkByText("""
foo = \a -> (\b -> b)

main : ()
main = <error descr="Type mismatch.Required: ()Found: String">foo "" ""</error>
""")

    fun `test lambda returning lambda returning lambda`() = checkByText("""
foo = \a -> (\b -> (\c -> c))

main : ()
main = <error descr="Type mismatch.Required: ()Found: String">foo "" "" ""</error>
""")

    fun `test record update in lambda`() = checkByText("""
type alias R = {x : ()}
main : R -> R
main = (\r -> { r | x = () })
""")

    fun `test matched lambda type with closure pattern matching`() = checkByText("""
type Foo = Bar
main : (Foo, Foo) -> ((Foo, Foo) -> Foo)
main a = (\(_, b) -> b)
""")

    fun `test mismatched lambda type with closure`() = checkByText("""
type Foo = Bar
main : () -> Foo -> Foo
main a = <error descr="Type mismatch.Required: Foo → FooFound: Foo → ()">(\_ -> a)</error>
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

    fun `test mismatched record subset pattern in parameter`() = checkByText("""
type alias Foo = { foo : (), bar : Int }
main : Foo -> ()
main {bar} = <error descr="Type mismatch.Required: ()Found: Int">bar</error>
""")

    // https://github.com/klazuka/intellij-elm/issues/122
    fun `test matched record pattern from extension alias`() = checkByText("""
type alias Foo a = { a | foo : ()}
type alias Bar = { bar : () }

main : Foo Bar -> ()
main {bar} = bar
""")

    fun `test mismatched record pattern from extension alias`() = checkByText("""
type alias Foo a = { a | foo : ()}
type alias Bar = { bar : () }

main : Foo Bar -> Int
main {bar} = <error descr="Type mismatch.Required: IntFound: ()">bar</error>
""")

    fun `test mismatched record pattern from extension alias redefining a field`() = checkByText("""
type alias Foo a = { a | foo : ()}
type alias Bar = Foo { foo : Int }

main : Bar -> Int
main {foo} = <error descr="Type mismatch.Required: IntFound: ()">foo</error>
""")

    fun `test extension record parameter`() = checkByText("""
type alias Extension base = { base | field : () }

foo : Extension base -> ()
foo e = ()

main = foo { field = (), field2 = () }
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

    // https://github.com/klazuka/intellij-elm/issues/153
    fun `test let-in with tuple with too small arity`() = checkByText("""
main : ()
main =
    let
        <error descr="Type mismatch.Required: ((), ())Found: (a, b, c)">(x, y, z)</error> = ((), ())
    in
        y
""")

    fun `test let-in with tuple with too large arity`() = checkByText("""
main : ()
main =
    let
        <error descr="Type mismatch.Required: ((), (), ())Found: (a, b)">(x, y)</error> = ((), (), ())
    in
        y
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
        <error descr="Type mismatch.Required: ()Found: { x : a, y : b }">{x, y}</error> = ()
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

    // #https://github.com/klazuka/intellij-elm/issues/142
    // this tests for infinite recursion; the diagnostic is tested in TypeDeclarationInspectionTest
    fun `test bad self-recursion in type alias`() = checkByText("""
type alias A = A
foo : A
foo = ()
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

    fun `test allowed self-recursion in lambda in unannotated function`() = checkByText("""
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

    // https://github.com/klazuka/intellij-elm/issues/113
    fun `test case branches with union value call`() = checkByText("""
foo : Maybe (List a)
foo = Nothing

main =
    case foo of
       Just [] -> ()
       _ -> ()
""")

    // https://github.com/klazuka/intellij-elm/issues/113
    fun `test field access on field subset`() = checkByText("""
type alias Subset a =
    { a | extra : () }


type alias Foo =
    { bar : () }

main : Subset Foo -> ()
main a =
    a.bar
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

    fun `test case branches using union patterns with record destructuring of var`() = checkByText("""
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
-- This will lead to unresolved reference errors, but we need to test that we're still
-- binding the parameters so that we can infer the branch expressions.
main arg =
    case arg of
        <error descr="Unresolved reference 'Bar'">Bar (Just {x})</error> -> x
        <error descr="Unresolved reference 'Baz'">Baz x</error> -> x
        _ -> ()
""")

    fun `test function parameters using union patterns to unresolved type`() = checkByText("""
<error descr="<module declaration> expected, got 'main'">main</error> <error descr="Unresolved reference 'Foo'">Foo bar</error> = <error descr="Value cannot be defined in terms of itself">bar</error>
""")

    fun `test case branch with cons pattern head`() = checkByText("""
main =
    case [()] of
        x :: xs -> x
        _ -> <error descr="Type mismatch.Required: ()Found: String">""</error>
""")

    fun `test case branch with cons pattern tail`() = checkByText("""
main : List ()
main =
    <error descr="Type mismatch.Required: List ()Found: List String">case [""] of
        x :: xs -> xs
        _ -> []</error>
""")

    fun `test case branch with list pattern`() = checkByText("""
main =
    case [()] of
        [x, y] -> y
        _ -> <error descr="Type mismatch.Required: ()Found: String">""</error>
""")

    fun `test case branch with cons and list pattern`() = checkByText("""
main =
    case [()] of
        z :: [x, y] -> y
        _ -> <error descr="Type mismatch.Required: ()Found: String">""</error>
""")

    fun `test case branches binding the same names`() = checkByText("""
type Foo a = Foo () | Bar String | Baz ()  | Qux () | Quz ()

bar : () -> ()
bar a = a

foo : Foo a -> ()
foo f =
    case f of
       Foo x -> bar x
       Bar x -> bar <error descr="Type mismatch.Required: ()Found: String">x</error>
       Baz x -> bar x
       Qux x -> bar x
       Quz x -> bar x
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

    // https://github.com/klazuka/intellij-elm/issues/247
    fun `test nested destructuring`() = checkByText("""
main : ()
main =
    let
        (a, b) =
            let
                (c, d) = ("", "")
            in
                (c, d)
    in
        <error descr="Type mismatch.Required: ()Found: String">a</error>
""")

    fun `test forward reference to destructured pattern`() = checkByText("""
main : ()
main =
    let
        a = b
        (b, c) = ("", "")
    in
       <error descr="Type mismatch.Required: ()Found: String">a</error>
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

    fun `test referenced pattern as`() = checkByText("""
main : { field : String } -> ()
main r =
    let
        ({field} as record) = r
    in
    <error descr="Type mismatch.Required: ()Found: String">record.field</error>
""")

    fun `test nested param as`() = checkByText("""
main : (String, String) -> ()
main ( (x) as foo, _ ) =
     <error descr="Type mismatch.Required: ()Found: String">foo</error>
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
apR : a -> (a -> b) -> b
apR x f = f x
infix left  0 (|>) = apR

main : Maybe ()
main = () |> Just
""")

    fun `test multiple non-associative operators`() = checkByText("""
lt : a -> a -> Bool
lt a b = True
and : Bool -> Bool -> Bool
and a b = False

infix right 3 (&&) = and
infix non   4 (<)  = lt

main : Bool
main = 1 < 2 && 3 < 4
""")

    fun `test operator mixed with function call`() = checkByText("""
type Foo = Bar
foo : Foo -> () -> Foo
foo a b = a
infix left 4 (~~) = foo

main = foo Bar () ~~ ()
""")

    fun `test self reference in union variant`() = checkByText("""
type Foo a = FooVariant Foo a
type Bar = BarVariant Bar (Foo Bar)

main : Foo ()
main = <error descr="Type mismatch.Required: Foo ()Found: Bar → Foo Bar → Bar">BarVariant</error>
""")

    // https://github.com/klazuka/intellij-elm/issues/201
    fun `test returning bound recursive union variant`() = checkByText("""
type Tree a = Tree a (List (Tree a))

directChildren : Tree a -> ()
directChildren tree =
    <error descr="Type mismatch.Required: ()Found: List (Tree a)">case tree of
        Tree _ children ->
            children</error>
""")

    fun `test mismatched return value from rigid vars`() = checkByText("""
foo : a -> a
foo a = a

main : Int
main = <error descr="Type mismatch.Required: IntFound: String">foo ""</error>
""")

    fun `test mismatched return value from nested vars`() = checkByText("""
foo : List a -> List a
foo a = a

main : List Int
main = <error descr="Type mismatch.Required: List IntFound: List String">foo [""]</error>
""")

    fun `test mismatched return value from multiple rigid vars`() = checkByText("""
foo : List a -> List b -> List b
foo a b = b

main : List Int
main = <error descr="Type mismatch.Required: List IntFound: List String">foo [1] [""]</error>
""")

    fun `test mismatched return value from doubly nested vars`() = checkByText("""
foo : List (List a) -> List (List a)
foo a = a

main : List (List Int)
main = <error descr="Type mismatch.Required: List (List Int)Found: List (List String)">foo [[""]]</error>
""")

    fun `test fixing var value at first occurrence`() = checkByText("""
foo : List a -> List a -> List a
foo a b = a

main : List String
main = foo [""] <error descr="Type mismatch.Required: List StringFound: List (List a)">[[]]</error>
""")

    fun `test passing vars through operator`() = checkByText("""
main : ()
main =
    <error descr="Type mismatch.Required: ()Found: String">List.head [""] |> Maybe.withDefault ""</error>
""")

    fun `test passing function type with vars`() = checkByText("""
map : (c -> d) -> List c -> List d
map f xs = []

foo : String -> String
foo s = s

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String → List String">map foo</error>
""")

    fun `test passing function type with vars to function`() = checkByText("""
appL : (a -> b) -> a -> b
appL f x = f x

map : (c -> d) -> List c -> List d
map f xs = []

foo : String -> String
foo s = s

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String → List String">appL map foo</error>
""")

    fun `test passing function type with vars to operator`() = checkByText("""
appL : (a -> b) -> a -> b
appL f x = f x
infix right 0 (<:) = appL

map : (c -> d) -> List c -> List d
map f xs = []

foo : String -> String
foo s = s

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String → List String">map <: foo</error>
""")

    fun `test passing record constructor to operator`() = checkByText("""
appR : a -> (a -> b) -> b
appR x f = f x
infix left 0 (:>) = appR

map : (c -> d) -> List c -> List d
map f xs = []

type alias Rec = { field : () }

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List () → List Rec">Rec :> map</error>
""")

    fun `test non function to composition operator`() = checkByText("""
compo : (b -> c) -> (a -> b) -> (a -> c)
compo g f x = g (f x)
infix left  9 (<<<) = compo

foo : d -> d
foo a = a

main =
    foo <<< <error descr="Type mismatch.Required: a → dFound: String">""</error>
""")


    fun `test multiple empty lists`() = checkByText("""
foo : z -> z -> z -> z
foo a b c = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String">foo [] [] [""]</error>
""")

    fun `test multiple mismatched lists`() = checkByText("""
foo : z -> z -> z -> z -> z
foo a b c d = a

main : ()
main =
    foo [] [] [""] <error descr="Type mismatch.Required: List StringFound: List ()">[()]</error>
""")

    fun `test multiple empty list functions ending with concrete type`() = checkByText("""
foo : z -> z -> z -> z
foo a b c = a

listA : List b -> List b
listA a = a

listStr : List String -> List String
listStr a = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String → List String">foo listA listA listStr</error>
""")

    fun `test multiple empty list functions starting with concrete type`() = checkByText("""
foo : z -> z -> z -> z
foo a b c = a

listA : List b -> List b
listA a = a

listStr : List String -> List String
listStr a = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String → List String">foo listStr listA listA</error>
""")

    fun `test function composition`() = checkByText("""
main : ()
main = <error descr="Type mismatch.Required: ()Found: String → Bool">String.isEmpty >> not</error>
""")

    fun `test constraint number int literals`() = checkByText("""
foo : number -> number -> number
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: number">foo 1 2</error>
""")

    fun `test constraint number float literals`() = checkByText("""
foo : number -> number -> number
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: Float">foo 1.1 2.2</error>
""")

    fun `test constraint number int and float literal`() = checkByText("""
foo : number -> number -> number
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: Float">foo 1 2.2</error>
""")

    fun `test constraint number number`() = checkByText("""
foo : number -> number -> number
foo a b = a

main : number -> number -> ()
main a b =
    <error descr="Type mismatch.Required: ()Found: number">foo a b</error>
""")

    fun `test constraint number mismatched`() = checkByText("""
foo : number -> number -> number
foo a b = a

main =
    foo 1 <error descr="Type mismatch.Required: numberFound: ()">()</error>
""")

    fun `test constraint appendable string`() = checkByText("""
foo : appendable -> appendable -> appendable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: String">foo "" ""</error>
""")

    fun `test constraint appendable list`() = checkByText("""
foo : appendable -> appendable -> appendable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List a">foo [] []</error>
""")

    fun `test constraint appendable appendable`() = checkByText("""
foo : appendable -> appendable -> appendable
foo a b = a

main : appendable -> appendable -> ()
main a b =
    <error descr="Type mismatch.Required: ()Found: appendable">foo a b</error>
""")

    fun `test constraint appendable string and list mismatch`() = checkByText("""
foo : appendable -> appendable -> appendable
foo a b = a

main =
    foo "" <error descr="Type mismatch.Required: StringFound: List a">[]</error>
""")

    fun `test constraint appendable list mismatch`() = checkByText("""
foo : appendable -> appendable -> appendable
foo a b = a

main =
    foo [""] <error descr="Type mismatch.Required: List StringFound: List ()">[()]</error>
""")

    fun `test constraint appendable number mismatch`() = checkByText("""
foo : appendable -> appendable
foo a = a

main =
    foo <error descr="Type mismatch.Required: appendableFound: number">1</error>
""")

    fun `test constraint comparable int`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : Int -> ()
main a =
    <error descr="Type mismatch.Required: ()Found: Int">foo a a</error>
""")

    fun `test constraint comparable float`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: Float">foo 1.1 2.2</error>
""")

    fun `test constraint comparable char`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: Char">foo 'a' 'b'</error>
""")

    fun `test constraint comparable string`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: String">foo "a" "b"</error>
""")

    fun `test constraint comparable list string`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String">foo ["a"] []</error>
""")

    fun `test constraint comparable tuple float`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: (Float, Float)">foo (1.1, 2.2) (3.3, 4.4)</error>
""")

    fun `test constraint comparable tuple mismatch`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : ()
main =
    foo ("", "") <error descr="Type mismatch.Required: (String, String)Found: (String, Float)">("", 1.1)</error>
""")

    fun `test constraint comparable comparable`() = checkByText("""
foo : comparable -> comparable -> comparable
foo a b = a

main : comparable -> comparable -> ()
main a b =
    <error descr="Type mismatch.Required: ()Found: comparable">foo a b</error>
""")

    fun `test constraint compappend string`() = checkByText("""
foo : comappend -> comappend -> comappend
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: String">foo "" ""</error>
""")

    fun `test constraint compappend list string`() = checkByText("""
foo : comappend -> comappend -> comappend
foo a b = a

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String">foo [""] []</error>
""")

    fun `test constraint compappend compappend`() = checkByText("""
foo : compappend -> compappend -> compappend
foo a b = a

main : compappend -> compappend -> ()
main a b =
    <error descr="Type mismatch.Required: ()Found: compappend">foo a b</error>
""")

    fun `test numbered constraint appendable`() = checkByText("""
foo : appendable1 -> appendable2 -> appendable3 -> appendable2
foo a b c = b

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: List String">foo "" [""] [()]</error>
""")

    fun `test numbered constraint appendable mismatch`() = checkByText("""
foo : appendable1 -> appendable2 -> appendable1 -> appendable2
foo a b c = b

main : ()
main =
    foo "" [""] <error descr="Type mismatch.Required: StringFound: List String">[""]</error>
""")

    fun `test calling function with its own return value`() = checkByText("""
type Foo a b = Foo
type Bar c = Bar

foo : Foo d (e -> f) -> Bar e -> Foo d f
foo a = Debug.todo ""

bar : Foo g g
bar = Foo

baz : Bar String
baz = Bar

qux : Bar Float
qux = Bar

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: Foo (String → Float → f) f">(foo (foo bar baz) qux)</error>
""")

    // https://github.com/klazuka/intellij-elm/issues/296
    fun `test tuple with repeated unfixed vars`() = checkByText("""
type alias Example = ( Maybe String, Maybe Int )

foo : Example -> Example
foo model = model

main : Example
main = ( Nothing, Nothing ) |> foo
""")

    fun `test constraining param via function call`() = checkByText("""
foo : Int -> Int
foo a = a

bar a = foo a

main : Int
main = bar <error descr="Type mismatch.Required: IntFound: String">""</error>
""")

    fun `test using constrained var in let`() = checkByText("""
bar : String -> String
bar a = a

main a =
    let
        x = a + 1
    in
        bar <error descr="Type mismatch.Required: StringFound: number">a</error>
""")

    fun `test using constrained var in lambda`() = checkByText("""
foo f = (\t -> f t)

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: (a → b) → a → b">foo</error>
""")

    fun `test using constrained var in multiple lambdas`() = checkByText("""
foo f x = if x then (\t -> f t) else (\_ -> f x)

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: (Bool → a) → Bool → Bool → a">foo</error>
""")

    fun `test using unconstrained var in record extension`() = checkByText("""
foo r = { r | field = () }

main : ()
main =
    <error descr="Type mismatch.Required: ()Found: { field : () }">foo { field = () }</error>
""")

    fun `test unconstrained case branch with unit pattern`() = checkByText("""
foo : String -> String
foo a = a
main a =
    let
        b = case a of
            () -> ()
    in
        foo <error descr="Type mismatch.Required: StringFound: ()">a</error>
""")

    fun `test unconstrained case branch with cons pattern head`() = checkByText("""
foo : () -> ()
foo a = a
main a =
    let
        b = case a of
            x :: xs -> x
            _ -> ""
    in
        ( foo <error descr="Type mismatch.Required: ()Found: List String">a</error>
        , foo <error descr="Type mismatch.Required: ()Found: String">b</error>
        )
""")

    fun `test unconstrained case branch with tuple pattern`() = checkByText("""
foo : () -> ()
foo a = a
main a =
    let
        b = case a of
            (x, y) -> x
            (x, ()) -> x
            _ -> ""
    in
        ( foo <error descr="Type mismatch.Required: ()Found: (String, ())">a</error>
        , foo <error descr="Type mismatch.Required: ()Found: String">b</error>
        )
""")

    //TODO remaining pattern types
}
