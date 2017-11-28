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
    fun isCore(): Boolean {
        val path = virtualFile?.path
        val isCore = path?.contains("packages/elm-lang/core/") ?: false
//        println("Checking path: $path, isCore=$isCore")
        return isCore
    }
}