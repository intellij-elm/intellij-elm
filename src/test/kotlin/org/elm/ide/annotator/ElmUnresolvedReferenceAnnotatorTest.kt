package org.elm.ide.annotator

class ElmUnresolvedReferenceAnnotatorTest: ElmAnnotatorTestBase() {


    fun `test unresolved refs`() = checkErrors("""
f = <error descr="Unresolved reference 'foobar'">foobar</error>

g : <error descr="Unresolved reference 'Foo'">Foo</error>
g = 42

h = <error descr="Unresolved reference 'Quux'">Quux</error>
""")


    fun `test unresolved qualified refs`() = checkErrors("""
f = <error descr="Unresolved reference 'Foo'">Foo.foobar</error>

g : <error descr="Unresolved reference 'Foo'">Foo.Bar</error>
g = 42

h = <error descr="Unresolved reference 'Foo'">Foo.Quux</error>
""")


    fun `test unresolved parametric type ref`() = checkErrors("""
f0 : <error descr="Unresolved reference 'Quux'">Quux</error> ()
f1 : <error descr="Unresolved reference 'Foo'">Foo.Quux</error> ()
""")


    fun `test built-in type refs have no errors`() = checkErrors("""
type alias MyList = List
""")


    fun `test type annotation record extension base variables have no errors`() = checkErrors("""
foo : { a | name : String } -> String
""")


    fun `test record extension base variables are checked for errors in type declarations`() = checkErrors("""
type alias Foo a = { <error descr="Unresolved reference 'b'">b</error> | name : String }
type Bar c = Bar { <error descr="Unresolved reference 'd'">d</error> | name : String }
""")


    fun `test qualified Kernel refs have no errors`() = checkErrors("""
import Elm.Kernel.Scheduler
f = Elm.Kernel.Scheduler.succeed
""")


    // TODO [drop 0.18] remove this test
    fun `test qualified Native refs have no errors`() = checkErrors("""
import Native.Scheduler
f = Native.Scheduler.succeed
""")


    fun `test type annotation refs have warning`() = checkWarnings("""
type Int = Placeholder
<weak_warning descr="'f' does not exist">f : Int -> Int</weak_warning>

g : Int
g = 0
""")


    fun testIssue93() = doTest("Issue93Module.elm")



    // LEGACY Elm 0.18 TESTS

    // TODO [drop 0.18] remove this test
    fun `test legacy built-in type refs have no errors`() = checkErrors("""
type alias MyStr = String
type alias MyInt = Int
type alias MyFloat = Float
type alias MyBool = Bool
type alias MyChar = Char
type alias MyList = List
""")

    // TODO [drop 0.18] remove this test
    fun `test legacy built-in value refs have no errors`() = checkErrors("""
x = True
y = False
""")
}