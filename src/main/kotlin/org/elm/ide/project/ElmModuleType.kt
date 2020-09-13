package org.elm.ide.project

import com.intellij.ide.util.projectWizard.EmptyModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.elm.ide.icons.ElmIcons
import javax.swing.Icon

/*
    DEPRECATED!

    We used to define a custom module as part of IntelliJ IDEA's new project wizard
    in order to provide a `ModuleBuilder`. But that doesn't work on WebStorm, and it was
    needlessly complicated by a bunch of Java junk. I am now standardizing on a "web project"
    technique that is simpler and works for both WebStorm and IDEA.

    The `moduleType` extension point is no longer strictly necessary, HOWEVER, if we were
    to remove it entirely, users who created an IntelliJ project using our *old* "new project"
    wizard, would see an "unknown module type" error when they open their project.
 */
class ElmModuleType : ModuleType<ModuleBuilder>(ID) {

    override fun createModuleBuilder() = EmptyModuleBuilder()

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