package org.frawa.elmtest.run;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.testframework.sm.FileUrlProvider;
import com.intellij.execution.testframework.sm.TestsLocationProviderUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.frawa.elmtest.core.LabelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.frawa.elmtest.core.ElmPluginHelper.getPsiElement;


public class ElmTestLocator extends FileUrlProvider {
    static final ElmTestLocator INSTANCE = new ElmTestLocator();

    private ElmTestLocator() {
    }

    @NotNull
    @Override
    public List<Location> getLocation(@NotNull String protocol, @NotNull String path, @Nullable String metainfo, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        if (!protocol.startsWith(LabelUtils.ELM_TEST_PROTOCOL)) {
            return super.getLocation(protocol, path, metainfo, project, scope);
        }

        if (protocol.startsWith(LabelUtils.ERROR_PROTOCOL)) {
            Pair<String, Pair<Integer, Integer>> pair = LabelUtils.fromErrorLocationUrlPath(path);
            String filePath = pair.first;
            int line = pair.second.first;
            int column = pair.second.second;

            final String systemIndependentPath = FileUtil.toSystemIndependentName(filePath);
            final List<VirtualFile> virtualFiles = TestsLocationProviderUtil.findSuitableFilesFor(systemIndependentPath, project);
            if (virtualFiles.isEmpty()) {
                return Collections.emptyList();
            }
            return virtualFiles.stream()
                    .map(vf -> getErrorLocation(line, column, project, vf))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        Pair<String, String> pair = LabelUtils.fromLocationUrlPath(path);
        String filePath = pair.first;
        String labels = pair.second;

        final String systemIndependentPath = FileUtil.toSystemIndependentName(filePath);
        final List<VirtualFile> virtualFiles = TestsLocationProviderUtil.findSuitableFilesFor(systemIndependentPath, project);
        if (virtualFiles.isEmpty()) {
            return Collections.emptyList();
        }

        boolean isDescribe = LabelUtils.DESCRIBE_PROTOCOL.equals(protocol);

        return virtualFiles.stream()
                .map(vf -> getLocation(isDescribe, labels, project, vf))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Location getLocation(boolean isDescribe, String labels, Project project, VirtualFile virtualFile) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return null;
        }

        PsiElement found = getPsiElement(isDescribe, labels, psiFile);
        return PsiLocation.fromPsiElement(project, found);
    }

    private Location getErrorLocation(int line, int column, Project project, VirtualFile virtualFile) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return null;
        }

        final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return PsiLocation.fromPsiElement(project, psiFile);
        }

        final int offset = document.getLineStartOffset(line - 1) + column - 1;
        final PsiElement element = psiFile.findElementAt(offset);
        if (element == null) {
            return PsiLocation.fromPsiElement(project, psiFile);
        }

        return PsiLocation.fromPsiElement(project, element);
    }
}
