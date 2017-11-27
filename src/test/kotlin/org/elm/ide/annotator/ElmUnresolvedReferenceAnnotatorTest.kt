package org.elm.ide.annotator

class ElmUnresolvedReferenceAnnotatorTest: ElmAnnotatorTestBase() {

    fun testUnresolvedRef() = checkErrors("""
f = <error descr="Unresolved reference 'foobar'">foobar</error>

g : <error descr="Unresolved reference 'Foo'">Foo</error>
g = 42

h = <error descr="Unresolved reference 'Quux'">Quux</error>
""")

    fun testUnresolvedQualifiedRef() = checkErrors("""
f = <error descr="Unresolved reference 'foobar'">Foo.foobar</error>

g : <error descr="Unresolved reference 'Bar'">Foo.Bar</error>
g = 42

h = <error descr="Unresolved reference 'Quux'">Foo.Quux</error>
""")

    fun testBuiltInTypesHaveNoErrors() = checkErrors("""
type alias MyStr = String
type alias MyInt = Int
type alias MyFloat = Float
type alias MyBool = Bool
type alias MyChar = Char
type alias MyList = List
""")
}