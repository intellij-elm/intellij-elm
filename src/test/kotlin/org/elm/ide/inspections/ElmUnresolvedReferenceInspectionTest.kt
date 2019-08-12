package org.elm.ide.inspections

class ElmUnresolvedReferenceInspectionTest: ElmInspectionsTestBase(ElmUnresolvedReferenceInspection()) {
    fun `test unresolved refs`() = checkByText("""
f = <error descr="Unresolved reference 'foobar'">foobar</error>

g : <error descr="Unresolved reference 'Foo'">Foo</error>
g = 42

h = <error descr="Unresolved reference 'Quux'">Quux</error>
""")


    fun `test unresolved qualified refs`() = checkByText("""
f = <error descr="Unresolved reference 'Foo'">Foo.foobar</error>

g : <error descr="Unresolved reference 'Foo'">Foo.Bar</error>
g = 42

h = <error descr="Unresolved reference 'Foo'">Foo.Quux</error>
""")


    fun `test unresolved type ref`() = checkByText("""
f0 : <error descr="Unresolved reference 'Quux'">Quux</error> ()
f1 : <error descr="Unresolved reference 'Foo'">Foo.Quux</error> ()
""")


    fun `test built-in type refs have no errors`() = checkByText("""
type alias MyList = List
""")


    fun `test type annotation record extension base variables have no errors`() = checkByText("""
foo : { a | name : String } -> String
""")

    fun `test type annotation variables have no errors`() = checkByText("""
foo : a -> b -> c
""")

    fun `test record extension base variables are checked for errors in type declarations`() = checkByText("""
type alias Foo a = { <error descr="Unresolved reference 'b'">b</error> | name : String }
type Bar c = Bar { <error descr="Unresolved reference 'd'">d</error> | name : String }
""")


    fun `test qualified Kernel refs have no errors`() = checkByText("""
import Elm.Kernel.Scheduler
f = Elm.Kernel.Scheduler.succeed
""")


    // TODO [drop 0.18] remove this test
    fun `test qualified Native refs have no errors`() = checkByText("""
import Native.Scheduler
f = Native.Scheduler.succeed
""")


    fun `test type annotation refs have warning`() = checkByText("""
type Int = Placeholder
<weak_warning descr="'f' does not exist">f : Int -> Int</weak_warning>

g : Int
g = 0
""")

    // When importing a module using an alias, as we do here, the original module
    // name can no longer be used. And since this might otherwise be confusing
    // to the user, we show a nice error message.
    fun `test names hidden by alias`() = checkByFileTree("""
--@ Main.elm
import Issue93Module as I
{-caret-}
type alias Foo =
    { a : <error descr="Unresolved reference 'A'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.A</error>
    , b : <error descr="Unresolved reference 'B'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.B ()</error>
    }

f a b = <error descr="Unresolved reference 'x'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.x</error>

g = <error descr="Unresolved reference 'ConstructorA'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.ConstructorA</error>

ok = I.x

badRefRegardlessOfAlias = <error descr="Unresolved reference 'bogus'">Issue93Module.bogus</error>    
--@ Issue93Module.elm
module Issue93Module exposing (..)

type A = ConstructorA
type B c = ConstructorB c

x : A
x = ConstructorA
""")


    // LEGACY Elm 0.18 TESTS

    // TODO [drop 0.18] remove this test
    fun `test legacy built-in type refs have no errors`() = checkByText("""
type alias MyStr = String
type alias MyInt = Int
type alias MyFloat = Float
type alias MyBool = Bool
type alias MyChar = Char
type alias MyList = List
""")

    // TODO [drop 0.18] remove this test
    fun `test legacy built-in value refs have no errors`() = checkByText("""
x = True
y = False
""")
}
