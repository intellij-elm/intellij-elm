package org.elm.lang.core.resolve

class ElmWildcardImportResolveTest : ElmResolveTestBase() {

    fun `test explicit import shadowing wildcard`() = stubOnlyResolve(
            """
--@ main.elm
import Foo exposing (..)
import Bar exposing (bar)
main = bar
       --^Bar.elm

--@ Foo.elm
module Foo exposing (..)
bar = 42

--@ Bar.elm
module Bar exposing (..)
bar = 99
""")

    fun `test explicit import shadowing wildcard 2`() = stubOnlyResolve(
            """
--@ main.elm
import Bar exposing (..)
import Foo exposing (bar)
main = bar
       --^Foo.elm

--@ Foo.elm
module Foo exposing (..)
bar = 42

--@ Bar.elm
module Bar exposing (..)
bar = 99
""")
}
