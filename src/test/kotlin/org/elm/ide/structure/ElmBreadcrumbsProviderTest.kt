package org.elm.ide.structure

import com.intellij.testFramework.UsefulTestCase
import org.elm.lang.ElmTestBase
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.descendantsOfType


internal class ElmBreadcrumbsProviderTest : ElmTestBase() {

    fun `test breadcrumbs`() {
        InlineFile("""
type alias A = {a = ()}
type T = U | V ()

main : a -> a -> ()
main _ _ =
    let
        foo = { rec | a = () }
        bar =
            case Nothing of
                Just () -> ()
                _ -> ()
    in
    if 1 + 2 == 3 then
        (\ -> ())
    else 
        (\a b (c, d) -> ())
""")

        val actual = myFixture.file.descendantsOfType<ElmPsiElement>()
                .mapNotNull { ElmBreadcrumbsProvider.breadcrumbName(it) }
                .joinToString(separator = "\n")

        val expected = """
            A
            T
            U
            V
            main :
            main
            let … in
            foo
            {rec | …}
            bar
            case Nothing of
            Just () ->
            _ ->
            if 1 + 2 == 3 then
        """.trimIndent()

        UsefulTestCase.assertSameLines(expected, actual)
    }
}
