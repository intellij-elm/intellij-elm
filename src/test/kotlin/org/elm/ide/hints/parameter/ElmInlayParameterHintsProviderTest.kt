/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.elm.ide.hints.parameter

import com.intellij.codeInsight.daemon.impl.HintRenderer
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language

class ElmInlayParameterHintsProviderTest : ElmTestBase() {
    fun `test fn args`() = checkByText("""module Foo exposing (..)

greet first last = "Hello " ++ first ++ " " ++ last

greeting =
    greet {-hint text="first:"-}"Jane" {-hint text="last:"-}"Doe"

""")

    fun `test no hints for destructured record arguments`() = checkByText("""module Foo exposing (..)

example =
    nameToString
        {-hint text="{ first, last }:"-}{ first = "Jane"
        , last = "Doe"
        }


nameToString { first, last } =
    first ++ " " ++ last
""")

    fun `test no hints for discarded arguments`() = checkByText("""module Foo exposing (..)

example =
    getUserId token <hint text="retry:"/>True

getUserId : Token -> Bool -> Cmd msg
getUserId _ retry =
    getUserIdHttpRequest retry
""")

    fun `test uses name from top-level as clause`() = checkByText("""module Foo exposing (..)
example =
    coordinatesToString {-hint text="coordinates:"-}( 1, 2 )

coordinatesToString (( x, y ) as coordinates) =
    String.fromInt x ++ ", " ++ String.fromInt y
""")

    fun `test uses variant name when destructuring custom type`() = checkByText("""module Foo exposing (..)

type Element = Element Internals

type alias Internals = { x: Int, y: Int }

example value=
    destructureVariant value


destructureVariant (Element internals) =
    "Example"
""")

    fun `test it discards nested parens in pattern expressions`() = checkByText("""module Foo exposing (..)

example3 =
    toString ( {-hint text="x:"-}123, {-hint text="y:"-}456)

toString (((x,y))) =
    String.fromInt x ++ ", " ++ String.fromInt y
""")

    fun `test show pattern matched value name inline`() = checkByText("""module Foo exposing (..)

example3 user =
    destructure ( {-hint text="{ first }:"-}user.fullName, {-hint text="age:"-}123 )


destructure ( { first }, age ) =
    first ++ ": " ++ String.fromInt age
""")
    fun `test as clause name overrides custom type variant name`() = checkByText("""module Foo exposing (..)

example =
    unfollowButton {-hint text="toMsg:"-}ClickedUnfollow {-hint text="cred:"-}cred {-hint text="author:"-}selected
    
unfollowButton toMsg cred ((FollowedAuthor uname _) as author) =
    toggleFollowButton "Unfollow"
        [ "btn-secondary" ]
        (toMsg cred author)
        uname
""")

    @Suppress("UnstableApiUsage")
    private fun checkByText(@Language("Elm") code: String) {
        InlineFile(code.replace(HINT_COMMENT_PATTERN, "<$1/>"))

        ElmInlayParameterHints.enabledOption.set(true)
        ElmInlayParameterHints.smartOption.set(true)

        myFixture.testInlays({ (it.renderer as HintRenderer).text }) { it.renderer is HintRenderer }
    }

    companion object {
        private val HINT_COMMENT_PATTERN = Regex("""\{-(hint.*?)-\}""")
    }
}