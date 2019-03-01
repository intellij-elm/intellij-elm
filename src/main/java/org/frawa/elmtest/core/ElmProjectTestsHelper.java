package org.frawa.elmtest.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.elm.workspace.ElmProject;
import org.elm.workspace.ElmWorkspaceService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class ElmProjectTestsHelper {

    private final ElmWorkspaceService workspaceService;

    public ElmProjectTestsHelper(Project project) {
        workspaceService = ServiceManager.getService(project, ElmWorkspaceService.class);
    }

    public Stream<String> allNames() {
        return getTestableProjects()
                .map(ElmProject::getPresentableName);
    }

    @NotNull
    private Stream<ElmProject> getTestableProjects() {
        return workspaceService.getAllProjects().stream()
                .filter(p -> Files.exists(p.getProjectDirPath().resolve("tests")));
    }

    public Optional<String> nameByProjectDirPath(String path) {
        return getTestableProjects()
                .filter(p -> p.getProjectDirPath().toString().equals(path))
                .map(ElmProject::getPresentableName)
                .findFirst();
    }

    public Optional<String> projectDirPathByName(String name) {
        return getTestableProjects()
                .filter(p -> p.getPresentableName().equals(name))
                .map(ElmProject::getProjectDirPath)
                .map(Path::toString)
                .findFirst();
    }

    public Optional<ElmProject> elmProjectByProjectDirPath(String path) {
        return getTestableProjects()
                .filter(p -> p.getProjectDirPath().toString().equals(path))
                .findFirst();
    }

    public Path adjustElmCompilerProjectDirPath(String elmFolder, Path compilerPath) {
        return elmProjectByProjectDirPath(elmFolder)
                .filter(ElmProject::isElm18)
                .map(_1 -> compilerPath.resolveSibling("elm-make"))
                .orElse(compilerPath);
    }

    public static Path elmFolderForTesting(ElmProject elmProject) {
        return elmProject.isElm18() && elmProject.getPresentableName().equals("tests")
                ? elmProject.getProjectDirPath().getParent()
                : elmProject.getProjectDirPath();
    }

}
