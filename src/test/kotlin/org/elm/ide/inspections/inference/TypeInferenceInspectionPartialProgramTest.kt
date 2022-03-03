package org.elm.ide.inspections.inference

import org.elm.ide.inspections.ElmInspectionsTestBase
import org.elm.ide.inspections.ElmTypeInferenceInspection

class TypeInferenceInspectionPartialProgramTest : ElmInspectionsTestBase(ElmTypeInferenceInspection()) {
    override fun getProjectDescriptor() = ElmWithStdlibDescriptor

    fun `test nested function without in branch`() = checkByText("""
main =
    let
        foo : ()
        foo =
            <error descr="Type mismatch.Required: ()Found: Float">1.0</error><EOLError descr="VIRTUAL_END_DECL or VIRTUAL_END_SECTION expected"></EOLError>
""")

    fun `test in expression without let branches`() = checkByText("""
main : ()
main =
    let
        foo<EOLError descr="COLON, EQ, LEFT_BRACE, LEFT_PARENTHESIS, LEFT_SQUARE_BRACKET, LOWER_CASE_IDENTIFIER, NUMBER_LITERAL, OPEN_CHAR, OPEN_QUOTE or UNDERSCORE expected"></EOLError>
    in
    <error descr="Type mismatch.Required: ()Found: Float">1.0</error>
""")

    fun `test in expression with reference to error decl`() = checkByText("""
main : ()
main =
    let
        foo =<error descr="<expr> expected, got '...'"> </error>...
    in
    foo
""")

    fun `test parenthesized in expression with reference to error decl`() = checkByText("""
main : ()
main =
    (let
        foo =<error descr="<expr> expected, got '...'"> </error>...
    in
    foo)
""")

    fun `test let decl with forward reference to error decl`() = checkByText("""
main : ()
main =
    let
        foo : ()
        foo =
            bar

        bar =<EOLError descr="<expr> expected, got '...'"></EOLError>
            ...
    in
        foo
""")

    fun `test let decl with backward reference to error decl`() = checkByText("""
main : ()
main =
    let
        bar =<EOLError descr="<expr> expected, got '...'"></EOLError>
            ...

        foo : ()
        foo =
            bar
    in
        foo
""")

    fun `test case with no branches`() = checkByText("""
main : ()
main =
    case ()<EOLError descr="<expr>, OF or OPERATOR_IDENTIFIER expected"></EOLError>
""")

    fun `test case branches with case error`() = checkByText("""
main : ()
main =
    case<error descr="<expr> expected, got 'of'"> </error> of
        1 -> <error descr="Type mismatch.Required: ()Found: String">""</error>
        _ -> <error descr="Type mismatch.Required: ()Found: number">1</error>
""")

    fun `test case branch with pattern error`() = checkByText("""
main : ()
main =
    case () of
        a<error descr="'::', ARROW or AS expected, got '.'">.</error> -> a
""")
}
