/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 *
 */

package org.elm.openapiext

sealed class Result<out T> {
    class Ok<out T>(val value: T) : Result<T>()
    class Err<out T>(val reason: String) : Result<T>()

    fun orNull(): T? {
        return when (this) {
            is Ok -> this.value
            is Err -> null
        }
    }
}
