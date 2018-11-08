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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.frawa.elmtest.core.LabelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ElmTestLocator extends FileUrlProvider {
    static final ElmTestLocator INSTANCE = new ElmTestLocator();

    private ElmTestLocator() {
    }

    @NotNull
    @Override
    public List<Location> getLocation(@NotNull String protocol, @NotNull String path, @Nullable String metainfo, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        if (!LabelUtils.ELM_TEST_PROTOCOL.equals(protocol)) {
            return super.getLocation(protocol, path, metainfo, project, scope);
        }

        Pair<String, String> pair = LabelUtils.fromLocationUrlPath(path);
        String filePath = pair.first;
        String label = String.format("\"%s\"", pair.second);

        final String systemIndependentPath = FileUtil.toSystemIndependentName(filePath);
        final List<VirtualFile> virtualFiles = TestsLocationProviderUtil.findSuitableFilesFor(systemIndependentPath, project);
        if (virtualFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Location> result = virtualFiles.stream()
                .flatMap(vf -> getLocations(label, project, vf))
                .collect(Collectors.toList());
        return result;
    }

    private Stream<Location> getLocations(String label, Project project, VirtualFile virtualFile) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return Stream.empty();
        }
        final Document doc = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (doc == null) {
            return Stream.empty();
        }

        String text = doc.getText();
        List<Integer> indices = new ArrayList<>();
        int current = 0;
        while (current > -1) {
            current = text.indexOf(label, current + 1);
            indices.add(current);
        }

        return indices.stream()
                .filter(index -> index > -1)
                .map(psiFile::findElementAt)
                .filter(Objects::nonNull)
                .map(element -> PsiLocation.fromPsiElement(project, element));
    }
}
