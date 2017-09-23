package org.elm.lang.core

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import org.elm.ide.icons.ElmIcons


object ElmLanguage : Language("Elm")


object ElmFileType : LanguageFileType(ElmLanguage) {
    override fun getIcon() =
            ElmIcons.file

    override fun getName() =
            "Elm file"

    override fun getDefaultExtension() =
            "elm"

    override fun getDescription() =
            "Elm language file"
}


class ElmFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(ElmFileType, "elm")
    }
}
