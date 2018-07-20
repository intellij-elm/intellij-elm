package org.elm.ide.docs

import org.intellij.lang.annotations.Language

class ElmQuickDocumentationTest : ElmDocumentationProviderTest() {
    fun `test variable declaration`() = doTest(
            """
foo = 0
--^
""",
            """
<div class='definition'><pre>foo</pre></div>
""")

    fun `test unary function`() = doTest(
            """
foo bar = bar
--^
""",
            """
<div class='definition'><pre>foo bar</pre></div>
""")

    fun `test binary function with line comment`() = doTest(
            """
--- this shouldn't be included
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre>foo bar baz</pre></div>
""")

    fun `test function with doc comment`() = doTest(
            """
{-| this should be included. -}
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre>foo bar baz</pre></div>
<div class='content'><p>this should be included.</p></div>
""")

    fun `test function with type annotation`() = doTest(
            """
foo : Int -> Int -> Int
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre>foo : Int -&gt; Int -&gt; Int
foo bar baz</pre></div>
""")

    fun `test function with type and docs`() = doTest(
            """
{-| foo some ints together -}
foo : Int -> Int -> Int
foo bar baz = bar baz
--^
""",
            """
<div class='definition'><pre>foo : Int -&gt; Int -&gt; Int
foo bar baz</pre></div>
<div class='content'><p>foo some ints together</p></div>
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
<div class='definition'><pre>foo : Int -&gt; Int -&gt; Int
foo bar baz</pre></div>
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
""")
    private fun doTest(@Language("Elm") code: String, @Language("Html") expected: String) =
            doTest(code, expected, ElmDocumentationProvider::generateDoc)
}
