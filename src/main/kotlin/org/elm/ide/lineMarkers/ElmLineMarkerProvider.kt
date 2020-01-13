package org.elm.ide.lineMarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.parentOfType

class ElmLineMarkerProvider : LineMarkerProviderDescriptor() {
    companion object {
        private val OPTIONS = arrayOf(ElmExposureLineMarkerProvider.OPTION, ElmRecursiveCallLineMarkerProvider.OPTION)
    }

    override fun getName(): String? = "Elm line markers"
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    // This provides the options to show in Settings > Editor > General > Gutter Icons
    override fun getOptions(): Array<Option> = OPTIONS

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        val first = elements.firstOrNull() ?: return
        if (DumbService.getInstance(first.project).isDumb || first.parentOfType<ElmFile>()?.elmProject == null) return

        // Create the providers each time this function is called, since they may have internal state
        val providers = mutableListOf<LineMarkerProvider>()
        if (ElmExposureLineMarkerProvider.OPTION.isEnabled) providers.add(ElmExposureLineMarkerProvider())
        if (ElmRecursiveCallLineMarkerProvider.OPTION.isEnabled) providers.add(ElmRecursiveCallLineMarkerProvider())

        if (providers.isEmpty()) return

        for (element in elements) {
            ProgressManager.checkCanceled()
            for (provider in providers) {
                provider.getLineMarkerInfo(element)?.let { result.add(it) }
            }
        }
    }
}
