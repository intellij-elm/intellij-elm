package org.elm.lang.core.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.ElmLanguage

class ElmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElmLanguage), PsiFile {

    override fun getFileType()=
            ElmFileType

    override fun toString() =
            "Elm File"

    override fun getIcon(flags: Int) =
            super.getIcon(flags)


    // TODO [kl] revisit how we determine this
    fun isCore() =
            virtualFile?.path?.contains("packages/elm-lang/core/") ?: false
}