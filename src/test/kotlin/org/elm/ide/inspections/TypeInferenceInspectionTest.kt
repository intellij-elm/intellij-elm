package org.elm.ide.inspections

class TypeInferenceInspectionTest : ElmInspectionsTestBase(TypeInferenceInspection()) {
    fun `test too many arguments to value`() = checkByText("""
foo = ()

main = <error descr="The `foo` value is not a function, but it was given 1 argument.">foo 1</error>
""")

    fun `test too many arguments to function`() = checkByText("""
foo a b = a

main = <error descr="The `foo` function expects 2 arguments, but it got 3 instead.">foo 1 2 3</error>
""")

    fun `test too many arguments to operator`() = checkByText("""
add a b = a
infix left 6 (+) = add
main = <error descr="The (+) operator expects 2 arguments, but it got 3 instead.">(+) 1 2 3</error>
""")

    fun `test mismatched int value type`() = checkByText("""
main : Int
main = <error descr="Type mismatch.Required: IntFound: ()">()</error>
""")

    fun `test mismatched tuple value type from missing field`() = checkByText("""
main : (String, String, String)
main = <error descr="Type mismatch.Required: (String, String, String)Found: (String, String)">("", "")</error>
""")

    fun `test mismatched tuple value type from wrong field type`() = checkByText("""
main : (String, String)
main = <error descr="Type mismatch.Required: (String, String)Found: (Float, String)">(1.0, "")</error>
""")

    fun `test mismatched tuple value type from extra field`() = checkByText("""
main : (String, String)
main = <error descr="Type mismatch.Required: (String, String)Found: (String, String, String)">("", "", "")</error>
""")

    fun `test matched tuple value type`() = checkByText("""
main : (Int, Int)
main = (1, 2)
""")

    fun `test mismatched return type from argument`() = checkByText("""
main : Int -> String
main a = <error descr="Type mismatch.Required: StringFound: Int">a</error>
""")

    fun `test mismatched return type from float literal`() = checkByText("""
main : Float -> String
main a = <error descr="Type mismatch.Required: StringFound: Float">1.0</error>
""")

    fun `test correct value type from record`() = checkByText("""
main : {x: Float, y: Float}
main = {x = 1.0, y = 2.0}
""")

    fun `test correct value type from record alias`() = checkByText("""
type alias A = {x: Float, y: Float}
main : A
main = {x = 1.0, y = 2.0}
""")

    fun `test mismatched value type from record subset`() = checkByText("""
type alias R = {x: Float, y: Float}
main : R
main = <error descr="Type mismatch.Required: {x: Float,y: Float}Found: {x: Float}">{x = 1.0}</error>
""")

    fun `test mismatched value type from record superset`() = checkByText("""
type alias R = {x: Float, y: Float}
main : R
main = <error descr="Type mismatch.Required: {x: Float,y: Float}Found: {x: Float,y: Float,z: Float}">{x = 1.0, y=2.0, z=3.0}</error>
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

    fun `test invalid constructor as type annotation`() = checkByText("""
type Maybe a = Just a | Nothing
main : <error descr="Unresolved reference 'Just'">Just a</error> -> ()
main a = ()
""")

    fun `test matched value from union constructor`() = checkByText("""
type Maybe = Just Int | Nothing
foo : (Int -> Maybe) -> () -> ()
foo _ = ()

main = foo Just ()
""")

    fun `test matched value from function call`() = checkByText("""
foo : a -> Int
foo _ = 1

main : Int
main = foo 1
""")

    fun `test matched value from function call with parenthesized arguments`() = checkByText("""
foo : a -> a -> Int
foo _ = 1

main : Int
main = foo (1) (2)
""")

    fun `test mismatched value from function call`() = checkByText("""
foo : a -> String
foo _ = ""

main : Int
main = <error descr="Type mismatch.Required: IntFound: String">foo 1</error>
""")

    fun `test matched function call from parameter`() = checkByText("""
main : (Float -> String) -> String
main fn = fn 1.0
""")

    fun `test mismatched function call from parameter`() = checkByText("""
main : (Float -> String) -> Int
main fn = <error descr="Type mismatch.Required: IntFound: String">fn 1.0</error>
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
}
