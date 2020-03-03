package org.elm.ide.inspections

class QuickFixInvocationTracker {
    @Volatile
    var invoked: Boolean = false
        private set

    fun invoke() {
        invoked = true
    }
}
