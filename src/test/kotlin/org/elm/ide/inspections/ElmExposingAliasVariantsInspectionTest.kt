package org.elm.ide.inspections


class ElmExposingAliasVariantsInspectionTest : ElmInspectionsTestBase(ElmExposingAliasVariantsInspection()) {
    fun `test fix`() = checkFixByFileTree("Remove (..)", """
--@ Main.elm
module Main exposing (..)
import Foo exposing (T(..), B, <error descr="Invalid (..) on alias import">A(..)</error>{-caret-}, U)

main : A -> B -> T -> U -> ()
main _ _ _ _ = ()
--@ Foo.elm
module Foo exposing (..)
type alias A = ()
type alias B = ()
type T = T
type U = U
""", """
module Main exposing (..)
import Foo exposing (T(..), B, A, U)

main : A -> B -> T -> U -> ()
main _ _ _ _ = ()
""")
}
