package org.frawa.elmtest.run;

import com.intellij.execution.testframework.autotest.AbstractAutoTestManager;
import com.intellij.execution.testframework.autotest.AutoTestWatcher;
import com.intellij.execution.testframework.autotest.DelayedDocumentWatcher;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.elm.lang.core.ElmFileType;
import org.jetbrains.annotations.NotNull;

@State(
        name = "ElmTestAutoTestManager",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)}
)
public class ElmTestAutoTestManager extends AbstractAutoTestManager {

    @NotNull
    static ElmTestAutoTestManager getInstance(Project project) {
        return ServiceManager.getService(project, ElmTestAutoTestManager.class);
    }

    ElmTestAutoTestManager(@NotNull Project project) {
        super(project);
    }

    @Override
    @NotNull
    protected AutoTestWatcher createWatcher(Project project) {
        return new DelayedDocumentWatcher(project,
                myDelayMillis,
                this::restartAllAutoTests,
                file -> FileEditorManager.getInstance(project).isFileOpen(file) &&
                        ElmFileType.INSTANCE.equals(file.getFileType())
        );
    }

}
