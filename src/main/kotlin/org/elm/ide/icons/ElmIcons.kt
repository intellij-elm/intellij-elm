package org.elm.ide.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ElmIcons {
    val FILE = getIcon("elm-file.png")
    val FUNCTION = getIcon("function.png")
    val VALUE = getIcon("value.png")
    val UNION_TYPE = getIcon("type.png")
    val TYPE_ALIAS = getIcon("type.png")

    private fun getIcon(path: String): Icon {
        return IconLoader.getIcon("/icons/$path")
    }
}