package org.elm.ide.search

import com.intellij.psi.PsiElement
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.psi.parentOfType

class ElmUsageTypeProvider : UsageTypeProviderEx {
    override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
        // targets?
        return if (element != null) getUsageType(element)
        else null
    }

    override fun getUsageType(element: PsiElement): UsageType? {
        return when (element) {
            is ElmFieldType -> UsageType.CLASS_FIELD_DECLARATION
            is ElmField -> UsageType.WRITE
            is ElmFieldAccessExpr -> UsageType.READ
            is ElmExposedType -> if (element.parentOfType<ElmImportClause>() != null) UsageType.CLASS_IMPORT
            else null /* export */
            else -> null
        }
    }
}