package org.elm.ide.annotator

class ElmUnresolvedReferenceAnnotatorTest: ElmAnnotatorTestBase() {


    fun `test unresolved refs`() = checkErrors("""
f = <error descr="Unresolved reference 'foobar'">foobar</error>

g : <error descr="Unresolved reference 'Foo'">Foo</error>
g = 42

h = <error descr="Unresolved reference 'Quux'">Quux</error>
""")


    fun `test unresolved qualified refs`() = checkErrors("""
f = <error descr="Unresolved reference 'Foo'"><error descr="Unresolved reference 'foobar'">Foo.foobar</error></error>

g : <error descr="Unresolved reference 'Foo'"><error descr="Unresolved reference 'Bar'">Foo.Bar</error></error>
g = 42

h = <error descr="Unresolved reference 'Foo'"><error descr="Unresolved reference 'Quux'">Foo.Quux</error></error>
""")


    fun `test built-in type refs have no errors`() = checkErrors("""
type alias MyList = List
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
}