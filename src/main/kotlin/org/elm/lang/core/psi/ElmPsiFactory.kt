package org.elm.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.psi.ext.childOfType
import org.elm.lang.core.psi.interfaces.ElmLowerCaseId
import org.elm.lang.core.psi.interfaces.ElmUpperCaseId

class ElmPsiFactory(private val project: Project)
{
    fun createLowerCaseIdentifier(text: String): ElmLowerCaseId =
            createFromText("$text = 42")
                    ?: error("Failed to create lower-case identifier: `$text`")

    fun createUpperCaseIdentifier(text: String): ElmUpperCaseId =
            createFromText("module $text exposing (..)")
                    ?: error("Failed to create upper-case identifier: `$text`")

    private inline fun <reified T : PsiElement> createFromText(code: String): T? =
            PsiFileFactory.getInstance(project)
                    .createFileFromText("DUMMY.elm", ElmFileType, code)
                    .childOfType<T>()
}