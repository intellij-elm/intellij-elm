package org.elm.ide.lineMarkers

class ElmExposureLineMarkerProviderTest : ElmLineMarkerProviderTestBase() {


    fun `test module that exposes all`() = doTestByText(
            """
module Main exposing (..)
n = 0               --> Exposed
f x = 0             --> Exposed
type Foo = Foo      --> Exposed (including variants)
type alias Bar = () --> Exposed
""")


    fun `test module that exposes a single function`() = doTestByText(
            """
module Main exposing (f)
f x = 0 --> Exposed
g x = 0
""")


    fun `test module that exposes a single value`() = doTestByText(
            """
module Main exposing (a)
a = 0 --> Exposed
b = 0
""")


    fun `test functions declared in a let-in do NOT get a line marker`() = doTestByText(
            """
module Main exposing (..)
f x = --> Exposed
    let y = 0
    in y
""")


    fun `test module that exposes a union type`() = doTestByText(
            """
module Main exposing (Foo)
type Foo = Foo --> Exposed
type Bar = Bar
""")


    fun `test module that exposes a union type and its constructors`() = doTestByText(
            """
module Main exposing (Foo(..))
type Foo = Foo --> Exposed (including variants)
""")


    fun `test module that exposes a type alias`() = doTestByText(
            """
module Main exposing (Foo)
type alias Foo = () --> Exposed
type alias Bar = ()
"""
    )


    fun `test module that exposes a port`() = doTestByText(
            """
port module Main exposing (foo)
port foo : a -> () --> Exposed
port bar : a -> ()
""")

}
