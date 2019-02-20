package org.frawa.elmtest.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.elm.workspace.ElmProject;
import org.elm.workspace.ElmWorkspaceService;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ElmProjectHelper {

    private final ElmWorkspaceService workspaceService;

    public ElmProjectHelper(Project project) {
        workspaceService = ServiceManager.getService(project, ElmWorkspaceService.class);
    }

    public Stream<String> allNames() {
        return workspaceService.getAllProjects().stream()
                .map(ElmProject::getPresentableName);
    }

    public Optional<String> nameByProjectDirPath(String path) {
        return workspaceService.getAllProjects().stream()
                .filter(p -> p.getProjectDirPath().toString().equals(path))
                .map(ElmProject::getPresentableName)
                .findFirst();
    }

    public Optional<String> projectDirPathByIndex(int index) {
        List<ElmProject> allProjects = workspaceService.getAllProjects();
        ElmProject project = 0 <= index && index < allProjects.size() ? allProjects.get(index) : null;
        return Optional.ofNullable(project)
                .map(ElmProject::getProjectDirPath)
                .map(Path::toString);
    }
}
