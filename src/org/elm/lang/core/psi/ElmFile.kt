package org.elm.lang.core.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.ElmLanguage

class ElmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElmLanguage) {

    override fun getFileType()=
            ElmFileType

    override fun toString() =
            "Elm File"

    override fun getIcon(flags: Int) =
            super.getIcon(flags)
}