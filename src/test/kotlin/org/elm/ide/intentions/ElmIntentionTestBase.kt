/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * From intellij-rust
 */

package org.elm.ide.intentions

import com.intellij.codeInsight.intention.IntentionAction
import org.elm.lang.ElmTestBase
import org.intellij.lang.annotations.Language


abstract class ElmIntentionTestBase(val intention: IntentionAction) : ElmTestBase() {

    protected fun doAvailableTest(@Language("Elm") before: String, @Language("Elm") after: String) {
        InlineFile(before).withCaret()
        myFixture.launchAction(intention)
        myFixture.checkResult(replaceCaretMarker(after))
    }

    protected fun doUnavailableTest(@Language("Elm") before: String) {
        InlineFile(before).withCaret()
        check(intention.familyName !in myFixture.availableIntentions.mapNotNull { it.familyName }) {
            "\"$intention\" intention should not be available"
        }
    }
}