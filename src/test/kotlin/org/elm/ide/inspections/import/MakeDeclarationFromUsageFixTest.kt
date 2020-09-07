package org.elm.ide.inspections.import

import com.intellij.openapi.vfs.VirtualFileFilter
import org.elm.fileTreeFromText
import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmUnresolvedReferenceInspection
import org.elm.lang.core.imports.ImportAdder.Import
import org.intellij.lang.annotations.Language

class MakeDeclarationFromUsageFixTest : ElmInspectionsTestBase(ElmUnresolvedReferenceInspection()) {
    fun `test basic top-level value`() = checkFixByTextWithoutHighlighting("Create",
"""module Foo exposing (..)

myString = greet{-caret-}

""",
            """module Foo exposing (..)

myString = greet
greet = Debug.todo "TODO"
"""

    )
}
