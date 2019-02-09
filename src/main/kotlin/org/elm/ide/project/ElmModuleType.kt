package org.elm.ide.project

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.elm.ide.icons.ElmIcons
import javax.swing.Icon

class ElmModuleType : ModuleType<ElmModuleBuilder>(ID) {

    override fun createModuleBuilder(): ElmModuleBuilder = ElmModuleBuilder()

    override fun getName(): String = "Elm"

    override fun getDescription(): String = "Elm module"

    override fun getNodeIcon(isOpened: Boolean): Icon = ElmIcons.FILE

    companion object {
        val ID = "ELM_MODULE"
        val INSTANCE: ElmModuleType by lazy {
            ModuleTypeManager.getInstance().findByID(ID) as ElmModuleType
        }
    }

}