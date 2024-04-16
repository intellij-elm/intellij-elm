package org.elm.lang.core.resolve

import org.junit.Test


class ElmLayeredImportResolveTest : ElmResolveTestBase() {

    /**
     * Layered imports are imports where multiple modules are imported using the same alias.
     */

    @Test
    fun `test layered import using first import`() = stubOnlyResolve(
            """
--@ main.elm
import Foo as F
import FooExtra as F
main = F.bar
         --^Foo.elm

--@ Foo.elm
module Foo exposing (..)
bar = 42

--@ FooExtra.elm
module FooExtra exposing (..)
quux = 99
""")

    @Test
    fun `test layered import using second import`() = stubOnlyResolve(
            """
--@ main.elm
import Foo as F
import FooExtra as F
main = F.quux
         --^FooExtra.elm

--@ Foo.elm
module Foo exposing (..)
bar = 42

--@ FooExtra.elm
module FooExtra exposing (..)
quux = 99
""")


}