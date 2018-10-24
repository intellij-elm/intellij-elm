package org.elm.ide.docs

import org.intellij.lang.annotations.Language

class ElmQuickDocumentationTest : ElmDocumentationProviderTest() {
    fun `test variable declaration`() = doTest(
            """
foo = 0
--^
""",
            """
<div class='definition'><pre><b>foo</b></pre></div>
""")

    fun `test unary function`() = doTest(
            """
foo bar = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> bar</pre></div>
""")

    fun `test binary function with line comment`() = doTest(
            """
--- this shouldn't be included
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> bar baz</pre></div>
""")

    fun `test binary function with as`() = doTest(
            """
foo (bar as baz) qux = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> (bar as baz) qux</pre></div>
""")

    fun `test function with doc comment`() = doTest(
            """
{-| this should be included. -}
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> bar baz</pre></div>
<div class='content'><p>this should be included.</p></div>
""")

    fun `test function with type annotation`() = doTest(
            """
foo : Int -> Int -> Int
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
""")

    fun `test function with qualified type annotation`() = doTest(
            """
foo : Int -> Http.Error
foo bar = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Http.Error">Http.Error</a>
<b>foo</b> bar</pre></div>
""")

    fun `test function with type and docs`() = doTest(
            """
{-| foo some ints together -}
foo : Int -> Int -> Int
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
<div class='content'><p>foo some ints together</p></div>
""")

    fun `test function in module`() = doTest(
            """
module Foo.Bar exposing (foo)

foo bar = bar
--^
""",
            """
<div class='definition'><pre><b>foo</b> bar<i> defined in </i>Foo.Bar</pre></div>
""")


    fun `test doc comments with markdown`() = doTest(
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
<div class='definition'><pre><b>foo</b> : <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Int">Int</a> -&gt; <a href="psi_element://Int">Int</a>
<b>foo</b> bar baz</pre></div>
<div class='content'><p>Map some <code>Int</code>s together,
producing another <code>Int</code></p><h2>Example</h2><pre><code>bar = 1
baz = 2
foo bar baz
</code></pre><p><em>For more information</em>, see <a href="https://example.com/">this</a> before
deciding if this is what you want.</p></div>
""")


    fun `test type declaration`() = doTest(
            """
type Foo = Bar
     --^
""",
            """
<div class='definition'><pre><b>type</b> Foo</pre></div>
<table class='sections'><tr><td valign='top' class='section'><p>Members:</td><td><p>
<p><code>Bar</code></td></table>
""")

    fun `test type declaration in module`() = doTest(
            """
module Foo.Bar exposing (Foo)

type Foo = Bar
     --^
""",
            """
<div class='definition'><pre><b>type</b> Foo<i> defined in </i>Foo.Bar</pre></div>
<table class='sections'><tr><td valign='top' class='section'><p>Members:</td><td><p>
<p><code>Bar</code></td></table>
""")

    fun `test type declaration with docs`() = doTest(
            """
{-| included *docs* -}
type Foo = Bar
     --^
""",
            """
<div class='definition'><pre><b>type</b> Foo</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
<table class='sections'><tr><td valign='top' class='section'><p>Members:</td><td><p>
<p><code>Bar</code></td></table>
""")

    fun `test type declaration with multiple members`() = doTest(
            """
{-| included *docs* -}
type Foo
     --^
     = Bar
     | Baz a
     | Qux (Maybe a) a
     | Lorem { ipsum: Dolor }
""",
            """
<div class='definition'><pre><b>type</b> Foo</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
<table class='sections'><tr><td valign='top' class='section'><p>Members:</td><td><p>
<p><code>Bar</code>
<p><code>Baz</code> a
<p><code>Qux</code> (<a href="psi_element://Maybe">Maybe</a> a) a
<p><code>Lorem</code> { ipsum : <a href="psi_element://Dolor">Dolor</a> }</td></table>
""")

    fun `test type alias`() = doTest(
            """
type alias Foo = Int
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
""")

    fun `test type alias in module`() = doTest(
            """
module Foo.Bar exposing (Foo)

type alias Foo = Int
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo<i> defined in </i>Foo.Bar</pre></div>
""")

    fun `test type alias with docs`() = doTest(
            """
{-| included *docs* -}
type alias Foo = Int
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
<div class='content'><p>included <em>docs</em></p></div>
""")

    fun `test type alias empty record`() = doTest(
            """
type alias Foo = { }
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
""")

    fun `test type alias record with fields`() = doTest(
            """
type alias Foo = { a: Int, b: String }
         --^
""",
            """
<div class='definition'><pre><b>type alias</b> Foo</pre></div>
<table class='sections'><tr><td valign='top' class='section'><p>Fields:</td><td><p>
<p><code>a</code> : <a href="psi_element://Int">Int</a>
<p><code>b</code> : <a href="psi_element://String">String</a></td></table>
""")

    fun `test module`() = doTest(
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
    fun `test module with docstring`() = doTest(
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

    fun `test function parameter`() = doTest(
            """
foo bar = ()
  --^
""",
            """
<div class='definition'><pre><i>parameter</i> bar <i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    fun `test function parameter with primitive type annotation`() = doTest(
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

// TODO[unification]
//    fun `test function parameter with nested parametric type annotation`() = doTest(
//            """
//type Foo a = Bar
//foo : Foo (Foo a) -> Foo (Foo a)
//foo bar = bar
//        --^
//""",
//            """
//<div class='definition'><pre><i>parameter</i> bar : <a href="psi_element://Foo">Foo</a> (<a href="psi_element://Foo">Foo</a> a)
//<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
//""")

    fun `test function parameter with parenthesized type annotation`() = doTest(
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

    fun `test function parameter with nested tuple type annotation`() = doTest(
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

    fun `test function parameter with record type annotation`() = doTest(
            """
type Int = Int
type Float = Float
foo : {x: Int, y: Float} -> Float
foo {x, y} = y
           --^
""",
            """
<div class='definition'><pre><i>parameter</i> y : <a href="psi_element://Float">Float</a>
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    fun `test function parameter with record type and as annotation`() = doTest(
            """
type Int = Int
type Float = Float
foo : {x: Int, y: Float} -> {x: Int, y: Float}
foo ({x, y} as z) = z
                  --^
""",
            """
<div class='definition'><pre><i>parameter</i> z : { x: <a href="psi_element://Int">Int</a>, y: <a href="psi_element://Float">Float</a> }
<i>of function </i><a href="psi_element://foo">foo</a></pre></div>
""")

    private fun doTest(@Language("Elm") code: String, @Language("Html") expected: String) =
            doTest(code, expected, ElmDocumentationProvider::generateDoc)
}
