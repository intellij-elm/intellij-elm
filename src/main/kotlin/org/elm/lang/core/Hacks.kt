package org.elm.lang.core

/**
 * Converts [moduleName] to a canonical form which should be used for lookup.
 */
fun moduleLookupHack(moduleName: String): String {
    /*
    The Elm compiler implicitly imports `Platform.Cmd` and `Platform.Sub`
    using the aliases `Cmd` and `Sub` respectively. So we index the aliased
    name instead of the full name as a hack to make it work with the rest of
    the reference/resolve system.

    TODO [kl] find a cleaner way to handle the implicit aliases
     */
    return when (moduleName) {
        "Platform.Cmd" -> "Cmd"
        "Platform.Sub" -> "Sub"
        else -> moduleName
    }
}