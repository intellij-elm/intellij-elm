package org.elm.ide.test.run

import org.elm.ide.test.run.ElmTestRunConfiguration.Companion.readOptions
import org.elm.ide.test.run.ElmTestRunConfiguration.Companion.writeOptions
import org.elm.ide.test.run.ElmTestRunConfiguration.Options
import org.jdom.Element
import org.junit.Assert.assertEquals
import org.junit.Test

class ElmTestRunConfigurationTest {

    @Test
    fun writeOptions() {
        val root = Element("ROOT")

        val options = Options()
        options.elmFolder = "folder"

        writeOptions(options, root)

        assertEquals(1, root.children.size.toLong())
        assertEquals(ElmTestRunConfiguration::class.java.simpleName, root.children[0].name)
        assertEquals(1, root.children[0].attributes.size.toLong())
        assertEquals("elm-folder", root.children[0].attributes[0].name)
        assertEquals("folder", root.children[0].attributes[0].value)
    }

    @Test
    fun roundTrip() {
        val root = Element("ROOT")

        val options = Options()
        options.elmFolder = "folder"

        writeOptions(options, root)
        val options2 = readOptions(root)

        assertEquals(options.elmFolder, options2.elmFolder)
    }

}