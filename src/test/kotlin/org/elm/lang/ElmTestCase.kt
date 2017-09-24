package org.elm.lang

interface ElmTestCase {

    companion object {
        val resourcesPath = "src/test/resources"
    }

    // Force the implementer to provide this method, which
    // is needed by IntelliJ's test base classes.
    // The implementation should be `fun getTestDataPath() = ElmTestCase.resourcesPath`
    fun getTestDataPath(): String
}
