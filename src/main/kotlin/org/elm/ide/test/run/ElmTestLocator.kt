package org.elm.ide.test.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.FileUrlProvider
import com.intellij.execution.testframework.sm.TestsLocationProviderUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.elm.ide.test.core.ElmPluginHelper
import org.elm.ide.test.core.ErrorLabelLocation
import org.elm.ide.test.core.LabelUtils
import org.elm.ide.test.core.LabelUtils.DESCRIBE_PROTOCOL
import org.elm.ide.test.core.LabelUtils.ERROR_PROTOCOL
import org.elm.ide.test.core.LabelUtils.TEST_PROTOCOL

/**
 * Provides a way to locate a test (group of functions in a test) in the IDE from the test results pane.
 */
object ElmTestLocator : FileUrlProvider() {

    override fun getLocation(protocol: String, path: String, metainfo: String?, project: Project, scope: GlobalSearchScope): List<Location<*>> {
        return when (protocol) {
            ERROR_PROTOCOL -> {
                val label = ErrorLabelLocation.fromUrl(path)
                val fileName = FileUtil.toSystemIndependentName(label.file)
                TestsLocationProviderUtil.findSuitableFilesFor(fileName, project)
                        .mapNotNull {
                            getErrorLocation(label.line, label.column, project, it)
                        }
            }

            DESCRIBE_PROTOCOL, TEST_PROTOCOL -> {
                val (filePath, labels) = LabelUtils.fromLocationUrlPath(path,
                        metainfo ?: error("missing path to tests dir"))
                val fileName = FileUtil.toSystemIndependentName(filePath)
                TestsLocationProviderUtil.findSuitableFilesFor(fileName, project)
                        .mapNotNull {
                            getLocation(protocol == DESCRIBE_PROTOCOL, labels, project, it)
                        }
            }

            else -> super.getLocation(protocol, path, metainfo, project, scope)
        }
    }

    private fun getLocation(isDescribe: Boolean, labels: String, project: Project, virtualFile: VirtualFile): Location<*>? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
        val found = ElmPluginHelper.getPsiElement(isDescribe, labels, psiFile)
        return PsiLocation.fromPsiElement(project, found)
    }

    private fun getErrorLocation(line: Int, column: Int, project: Project, virtualFile: VirtualFile): Location<*>? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null

        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: return PsiLocation.fromPsiElement(project, psiFile)

        val offset = document.getLineStartOffset(line - 1) + column - 1
        val element = psiFile.findElementAt(offset) ?: return PsiLocation.fromPsiElement(project, psiFile)

        return PsiLocation.fromPsiElement(project, element)
    }
}