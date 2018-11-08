package org.elm.ide.usages

import com.intellij.usages.Usage
import com.intellij.usages.UsageTarget
import com.intellij.usages.rules.ImportFilteringRule
import com.intellij.usages.rules.PsiElementUsage
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.parentOfType

/**
 * A filtering rule that allows you exclude imports from actions such as "find usages"
 */
class ElmImportFilteringRule : ImportFilteringRule() {
    override fun isVisible(usage: Usage, targets: Array<UsageTarget>): Boolean {
        val element = (usage as? PsiElementUsage)?.element
        return element?.containingFile !is ElmFile
                || (element.parentOfType<ElmImportClause>() == null
                && element.parentOfType<ElmModuleDeclaration>() == null
                && element !is ElmTypeAnnotation)
    }
}
