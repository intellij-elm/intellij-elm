package org.elm.ide.docs

import org.intellij.lang.annotations.Language
import org.junit.Test

class ElmQuickDocumentationTest : ElmDocumentationProviderTest() {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    @Test
    fun `test variable declaration (flaky)`() = doTest(
            """
foo = 0
--^
""",
            """
<div class='definition'><pre><b>foo</b> : number
<b>foo</b></pre></div>
""")

    @Test
    fun `test unary function (flaky)`() = doTest(
            """
foo bar = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> : a → a
<b>foo</b> bar</pre></div>
""")

    @Test
    fun `test binary function with line comment (flaky)`() = doTest(
            """
--- this shouldn't be included
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> : (b → a) → b → a
<b>foo</b> bar baz</pre></div>
""")

    @Test
    fun `test binary function with as (flaky)`() = doTest(
            """
foo (bar as baz) qux = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> : a → b → a
<b>foo</b> (bar as baz) qux</pre></div>
""")

    @Test
    fun `test unannotated function (flaky)`() = doTest(
            """
foo a = ((), "", a + 1)
main = foo
      --^
""",
            """
<div class='definition'><pre><b>foo</b> : number → ((), <a href="psi_element://String">String</a>, number)
<b>foo</b> a</pre></div>
""")

    @Test
    fun `test var with later constraints (flaky)`() = doTest(
            """
foo a =
    let
        b = a
          --^
        c = a ++ ""
    in
        a

""",
            """
<div class='definition'><pre><i>parameter</i> a : <a href="psi_element://String">String</a>
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    @Test
    fun `test function with doc comment (flaky)`() = doTest(
            """
{-| this should be included. -}
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> : (b → a) → b → a
<b>foo</b> bar baz</pre></div>
<div class='content'><p>this should be included.</p></div>
""")

    @Test
    fun `test function with type annotation (flaky)`() = doTest(
            """
foo : Int -> Int -> Int
foo bar baz = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
""")

    @Test
    fun `test function with type annotation with nested types (flaky)`() = doTest(
            """
foo : List (List a) -> ()
foo bar = ()
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://List">List</a> (<a href="psi_element://List">List</a> a) → ()
<b>foo</b> bar</pre></div>
""")

    @Test
    fun `test function with type annotation and parameterized alias (flaky)`() = doTest(
            """
type alias A a = {x: a, y: ()}
main : A ()
main = {x = (), y = ()}
--^
""",
            """
<div class='definition'><pre><b>main</b> : <a href="psi_element://A">A</a> ()
<b>main</b></pre></div>
""")

    @Test
    fun `test nested function with type annotation (flaky)`() = doTest(
            """
main a =
    let
        foo : Int -> Int -> Int
        foo bar baz = a
    in
        foo
        --^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
""")

    @Test
    fun `test function in let (flaky)`() = doTest(
            """
foo a =
    let
      bar b = b + 1
      --^
    in
        a
""",
            """
<div class='definition'><pre><b>bar</b> : number → number
<b>bar</b> b</pre></div>
""")


    @Test
    fun `test function with qualified type annotation (flaky) (flaky)`() = doTest(
            """
import Json.Decode
foo : Json.Decode.Decoder ()
foo = Json.Decode.succeed ()
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Decoder">Decoder</a> ()
<b>foo</b></pre></div>
""")

    @Test
    fun `test function with type and docs (flaky)`() = doTest(
            """
{-| foo some ints together -}
foo : Int -> Int -> Int
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
<div class='content'><p>foo some ints together</p></div>
""")

    @Test
    fun `test function in module (flaky)`() = doTest(
            """
module Foo.Bar exposing (foo)

foo bar = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> : a → a
<b>foo</b> bar<i> defined in </i>Foo.Bar</pre></div>
""")


    @Test
    fun `test doc comments with markdown (flaky)`() = doTest(
            """
{-| Map some `Int`s together,
producing another `Int`

# Example

    bar = 1
    baz = 2
    foo bar baz

*For more information*, see [this][link] before
deciding if this is what you want.

[link]: https://example.com/
-}
foo : Int -> Int -> Int
foo bar baz =
--^
  bar baz
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a> → <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
<div class='content'><p>Map some <code>Int</code>s together,
producing another <code>Int</code></p><h2>Example</h2><pre><code>bar = 1
baz = 2
foo bar baz
</code></pre><p><em>For more information</em>, see <a href="https://example.com/">this</a> before
deciding if this is what you want.</p></div>
""")


    @Test
    fun `test type declaration (flaky)`() = doTest(
            """
type Foo = Bar
     --^
""",
            """
<div class='definition'><pre><b>type</b> Foo</pre></div>
<table class='sections'><tr><td valign='top' class='section'><p>Variants:</td><td valign='top'><p>
<p><code>Bar</code></td></table>
""")

    @Test
    fun `test type declaration in module (flaky)`() = doTest(
            """
module Foo.Bar exposing (Foo)

type Foo = Bar
     --^
""",
            """
<div class='definition'><pre><b>type</b> Foo<i> defined in </i>Foo.Bar</pre></div>
<table class='sections'><tr><td valign='top' class='section'><p>Variants:</td><td valign='top'><p>
<p><code>Bar</code></td></table>
""")

    @Test
    fun `test type declaration with docs (flaky)`() = doTest(
            """
{-| included *docs* -}
type Foo = Bar
     --^
""",
            """
<div class='definition'><pre><b>type</b> Foo</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
<table class='sections'><tr><td valign='top' class='section'><p>Variants:</td><td valign='top'><p>
<p><code>Bar</code></td></table>
""")

    @Test
    fun `test type declaration with multiple variants (flaky)`() = doTest(
            """
{-| included *docs* -}
type Foo
     --^
     = Bar
     | Baz Foo
     | Qux (List a) a
     | Lorem { ipsum: Int }
""",
            """
<div class='definition'><pre><b>type</b> Foo</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
<table class='sections'><tr><td valign='top' class='section'><p>Variants:</td><td valign='top'><p>
<p><code>Bar</code>
<p><code>Baz</code> <a href="psi_element://Foo">Foo</a>
<p><code>Qux</code> (<a href="psi_element://List">List</a> a) a
<p><code>Lorem</code> { ipsum : <a href="psi_element://Int">Int</a> }</td></table>
""")

    @Test
    fun `test union variant with parameters (flaky)`() = doTest(
            """
type Foo a = Bar | Baz a (List Int) Int
                 --^
""",
            """
<div class='definition'><pre><i>variant</i> Baz a (<a href="psi_element://List">List</a> <a href="psi_element://Int">Int</a>) <a href="psi_element://Int">Int</a><i> of type </i><a href="psi_element://Foo">Foo</a></pre></div>
""")

    @Test
    fun `test union variant without parameters (flaky)`() = doTest(
            """
type Foo a = Bar | Baz a Foo
             --^
""",
            """
<div class='definition'><pre><i>variant</i> Bar<i> of type </i><a href="psi_element://Foo">Foo</a></pre></div>
""")

    @Test
    fun `test type alias (flaky)`() = doTest(
            """
type alias Foo = Int
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
""")

    @Test
    fun `test type alias in module (flaky)`() = doTest(
            """
module Foo.Bar exposing (Foo)

type alias Foo = Int
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo<i> defined in </i>Foo.Bar</pre></div>
""")

    @Test
    fun `test type alias with docs (flaky)`() = doTest(
            """
{-| included *docs* -}
type alias Foo = Int
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
""")

    @Test
    fun `test type alias empty record (flaky)`() = doTest(
            """
type alias Foo = { }
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
""")

    @Test
    fun `test type alias record with fields (flaky)`() = doTest(
            """
type alias Foo = { a: Int, b: String }
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
<table class='sections'><tr><td valign='top' class='section'><p>Fields:</td><td valign='top'><p>
<p><code>a</code> : <a href="psi_element://Int">Int</a>
<p><code>b</code> : <a href="psi_element://String">String</a></td></table>
""")

    @Test
    fun `test module (flaky)`() = doTest(
            """
module Main exposing (main)
      --^
main = ()
""",
            """
<div class='definition'><pre><i>module</i> Main</pre></div>
""")

    // This test is kludgy: since a line comment before a doc comment will cause the doc comment to fail to attach to
    // the module element, we need to put the line comment inside the doc comment.
    @Test
    fun `test module with docstring (flaky)`() = doTest(
            """
module Main exposing (main)
{-|  --^

Module docs

# Header
@docs main, foo, Bar

# Helpers
@docs main, foo,
      Bar, Baz
-}
main = ()
foo = ()
type alias Bar = ()
type Baz = Baz
""",
            """
<div class='definition'><pre><i>module</i> Main</pre></div>
<div class='content'><p>--^</p><p>Module docs</p><h2>Header</h2><a href="psi_element://main">main</a>, <a href="psi_element://foo">foo</a>, <a href="psi_element://Bar">Bar</a><h2>Helpers</h2><a href="psi_element://main">main</a>, <a href="psi_element://foo">foo</a>, <a href="psi_element://Bar">Bar</a>, <a href="psi_element://Baz">Baz</a></div>
""")

    @Test
    fun `test function parameter (flaky)`() = doTest(
            """
foo bar = ()
  --^
""",
            """
<div class='definition'><pre><i>parameter</i> bar : a
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    @Test
    fun `test function parameter with primitive type annotation (flaky)`() = doTest(
            """
type Int = Int
foo : Int -> Int
foo bar = bar
        --^
""",
            """
<div class='definition'><pre><i>parameter</i> bar : <a href="psi_element://Int">Int</a>
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    @Test
    fun `test function parameter with nested parametric type annotation (flaky)`() = doTest(
            """
type Foo a = Bar
foo : Foo (Foo a) -> Foo (Foo a)
foo bar = bar
        --^
""",
            """
<div class='definition'><pre><i>parameter</i> bar : <a href="psi_element://Foo">Foo</a> (<a href="psi_element://Foo">Foo</a> a)
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    @Test
    fun `test function parameter with parenthesized type annotation (flaky)`() = doTest(
            """
type Int = Int
foo : ((Int)) -> Int
foo ((bar)) = bar
            --^
""",
            """
<div class='definition'><pre><i>parameter</i> bar : <a href="psi_element://Int">Int</a>
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    @Test
    fun `test function parameter with nested tuple type annotation (flaky)`() = doTest(
            """
type Int = Int
type String = String
type Float = Float
foo : (Int, (String, Float)) -> String
foo (_, (bar, _)) = bar
                  --^
""",
            """
<div class='definition'><pre><i>parameter</i> bar : <a href="psi_element://String">String</a>
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

// The value now resolves to the field inside the annotation, which we don't have a ty for.
//    fun `test function parameter with record type annotation (flaky)`() = doTest(
//            """
//type Int = Int
//type Float = Float
//foo : {x: Int, y: Float} -> Float
//foo {x, y} = y
//           --^
//""",
//            """
//<div class='definition'><pre><i>parameter</i> y : <a href="psi_element://Float">Float</a>
//<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
//""")

    @Test
    fun `test function parameter with record type and as annotation (flaky)`() = doTest(
            """
type Int = Int
type Float = Float
foo : {x: Int, y: Float} -> {x: Int, y: Float}
foo ({x, y} as z) = z
                  --^
""",
            """
<div class='definition'><pre><i>parameter</i> z : { x : <a href="psi_element://Int">Int</a>, y : <a href="psi_element://Float">Float</a> }
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    @Test
    fun `test aliased types (flaky)`() = doTest(
            """
type alias T1 t = ()
type alias T2 u = T1 t
foo : T1 a -> T2 b
foo a = a
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://T1">T1</a> t → <a href="psi_element://T2">T2</a> u
<b>foo</b> a</pre></div>
""")

    @Test
    fun `test alias to unresolved type (flaky)`() = doTest(
            """
type alias Html msg = VirtualDom.Node msg
foo : Html msg -> Html msg
foo a = a
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Html">Html</a> msg → <a href="psi_element://Html">Html</a> msg
<b>foo</b> a</pre></div>
""")

    @Test
    fun `test operator (flaky)`() = doTest(
            """
{-| included *docs* -}
foo : number -> number -> number
foo a b = a
infix left  6 (~~)  = foo

bar = 11 ~~ 11
        --^
""",
            """
<div class='definition'><pre><b>foo</b> : number → number → number
<b>foo</b> a b</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
""")

    private fun doTest(@Language("Elm") code: String, @Language("Html") expected: String) =
            doTest(code, expected, ElmDocumentationProvider::generateDoc)
}
