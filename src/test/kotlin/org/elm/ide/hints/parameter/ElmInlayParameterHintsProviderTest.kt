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
        { first = "Jane"
        , last = "Doe"
        }


nameToString { first, last } =
    first ++ " " ++ last
""")

    fun `test no hints for nested record destructuring`() = checkByText("""module Foo exposing (..)

example =
    nameToString ( (), { first = "Jane" , last = "Doe" } )


nameToString (_, { first, last }) =
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
    destructure ( user.fullName, {-hint text="age:"-}123 )


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


    fun `test doesn't show hints for unit destructure`() = checkByText("""module Foo exposing (..)

example =
    destructureUnit ()


destructureUnit () =
    ()
""")

    fun `test type hint`() = checkByText("""module Foo exposing (..)

example () =
    call {-hint text="fn:"-}(\(greeting{-hint text=": String"-}, name{-hint text=": String"-}) -> greeting ++ ", " ++ name ) {-hint text="value:"-}("Hello", "World")

call fn value = 
    fn value
""")

    fun `test let binding type hint`() = checkByText("""module Foo exposing (..)

example =
    let
        foo ={-hint text=" -- Float"-}
            3.14
    in
    foo
""")

    fun `test no hint for annotated let binding`() = checkByText("""module Foo exposing (..)

example =
    let
        foo : List Float
        foo =
            1 :: [ 2.3 ]
    in
    foo
""")

    fun `test don't show type hint when unknown`() = checkByText("""module Foo exposing (..)

example =
    let
        foo =
            123 + ""
    in
    foo
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