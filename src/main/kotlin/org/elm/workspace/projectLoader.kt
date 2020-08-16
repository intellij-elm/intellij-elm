package org.elm.workspace

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import org.elm.openapiext.findFileByPathTestAware
import org.elm.workspace.solver.Pkg
import org.elm.workspace.solver.PkgName
import org.elm.workspace.solver.Repository
import org.elm.workspace.solver.solve
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val objectMapper = ObjectMapper()

class ElmPackageRepository(val elmCompilerVersion: Version) : Repository {

    private val inMemCache: MutableMap<String, List<Pkg>> = mutableMapOf()

    val elmHomePath: String
        get() {
            /*
            The Elm compiler first checks the ELM_HOME environment variable. If not found,
            it will fallback to the path returned by Haskell's `System.Directory.getAppUserDataDirectory`
            function. That function behaves as follows:

            - On Unix-like systems, the path is ~/.<app>.
            - On Windows, the path is %APPDATA%/<app> (e.g. C:/Users/<user>/AppData/Roaming/<app>)

            IntelliJ's FileUtil.expandUserHome() uses the JVM's `user.home` system property to
            determine the home directory.

            - On Unix-like systems, the path is /Users/<user>
            - on Windows, the path is C:/Users/<user>

            Note that the Haskell and Java functions do slightly different things.
            */
            val elmHomeVar = System.getenv("ELM_HOME")
            if (elmHomeVar != null && Paths.get(elmHomeVar).exists())
                return elmHomeVar

            return when {
                SystemInfo.isUnix -> FileUtil.expandUserHome("~/.elm")
                SystemInfo.isMac -> FileUtil.expandUserHome("~/.elm")
                SystemInfo.isWindows -> FileUtil.expandUserHome("~/AppData/Roaming/elm")
                else -> error("Unsupported platform")
            }
        }

    /**
     * Path to Elm's global package cache directory
     */
    private val globalPackageCacheDir: Path
        get() {
            // In 0.19.0, the directory name was singular, but beginning in 0.19.1 it is now plural
            val subDirName = when (elmCompilerVersion) {
                Version(0, 19, 0) -> "package"
                else -> "packages"
            }
            return Paths.get("$elmHomePath/$elmCompilerVersion/$subDirName/")
        }

    /**
     * Path to the manifest file for the Elm package [name] at version [version]
     */
    fun findPackageManifest(name: String, version: Version): Path {
        return globalPackageCacheDir.resolve("$name/$version/${ElmToolchain.ELM_JSON}")
    }

    override fun get(name: PkgName): List<Pkg> =
            inMemCache.getOrPut(name) {
                (File("$globalPackageCacheDir/$name/").listFiles() ?: emptyArray())
                        .mapNotNull { dir ->
                            val version = Version.parseOrNull(dir.name) ?: return@mapNotNull null
                            val proj = parseDTO(dir.toPath().resolve("elm.json")) as ElmPackageProjectDTO
                            object : Pkg {
                                override val name: PkgName = name
                                override val version: Version = version
                                override val elmVersion: Constraint = proj.elmVersion
                                override val dependencies: Map<PkgName, Constraint> = proj.dependencies
                            }
                        }
            }
}


class ElmProjectLoader(
        private val repo: ElmPackageRepository,
        private val versionsByPackage: Map<String, Version>
) {
    private fun load(packageName: String): ElmPackageProject {
        val version = versionsByPackage[packageName]
                ?: throw ProjectLoadException("Could not find suitable version of $packageName")
        val manifestPath = repo.findPackageManifest(packageName, version)
        return when (val dto = parseDTO(manifestPath)) {
            is ElmPackageProjectDTO ->
                ElmPackageProject(
                        manifestPath = manifestPath,
                        elmVersion = dto.elmVersion,
                        dependencies = dto.dependencies.keys.map { load(it) },
                        testDependencies = dto.testDependencies.keys.map { load(it) },
                        sourceDirectories = listOf(Paths.get("src")),
                        name = dto.name,
                        version = dto.version,
                        exposedModules = dto.exposedModulesNode.toExposedModuleMap())
            else -> error("should never happen")
        }
    }

    companion object {
        fun topLevelLoad(manifestPath: Path, repo: ElmPackageRepository): ElmProject {
            return when (val dto = parseDTO(manifestPath)) {
                is ElmApplicationProjectDTO -> {
                    val deps = dto.dependencies.direct + dto.dependencies.indirect +
                            dto.testDependencies.direct + dto.testDependencies.indirect
                    val loader = ElmProjectLoader(repo, deps)
                    ElmApplicationProject(
                            manifestPath = manifestPath,
                            elmVersion = dto.elmVersion,
                            dependencies = dto.dependencies.direct.keys.map { loader.load(it) },
                            testDependencies = dto.testDependencies.direct.keys.map { loader.load(it) },
                            sourceDirectories = dto.sourceDirectories,
                            testsRelativeDirPath = DEFAULT_TESTS_DIR_NAME
                    )
                }
                is ElmPackageProjectDTO -> {
                    val deps = solve(dto.dependencies + dto.testDependencies, repo)
                            ?: throw ProjectLoadException("unsolvable constraints")
                    val loader = ElmProjectLoader(repo, deps)
                    ElmPackageProject(
                            manifestPath = manifestPath,
                            elmVersion = dto.elmVersion,
                            dependencies = dto.dependencies.keys.map { loader.load(it) },
                            testDependencies = dto.testDependencies.keys.map { loader.load(it) },
                            sourceDirectories = listOf(Paths.get("src")),
                            name = dto.name,
                            version = dto.version,
                            exposedModules = dto.exposedModulesNode.toExposedModuleMap())
                }
            }
        }
    }
}


private fun parseDTO(manifestPath: Path): ElmProjectDTO {
    val manifestStream = findFileByPathTestAware(manifestPath)?.inputStream
            ?: throw ProjectLoadException("Manifest file not found: $manifestPath")
    return try {
        val node = objectMapper.readTree(manifestStream)
        when (val type = node.get("type")?.textValue()) {
            "application" -> objectMapper.treeToValue(node, ElmApplicationProjectDTO::class.java)
            "package" -> objectMapper.treeToValue(node, ElmPackageProjectDTO::class.java)
            else -> throw ProjectLoadException("Invalid elm.json: unexpected type '$type'")
        }
    } catch (e: JsonProcessingException) {
        throw ProjectLoadException("Invalid elm.json: ${e.message}")
    }
}

// DTOs for JSON Decoding

@JsonIgnoreProperties(ignoreUnknown = true)
private sealed class ElmProjectDTO


private class ElmApplicationProjectDTO(
        @JsonProperty("elm-version") val elmVersion: Version,
        @JsonProperty("source-directories") val sourceDirectories: List<Path>,
        @JsonProperty("dependencies") val dependencies: ExactDependenciesDTO,
        @JsonProperty("test-dependencies") val testDependencies: ExactDependenciesDTO
) : ElmProjectDTO()


/**
 * DTO used to wrap the data in `elm.intellij.json`.
 *
 * @see [ElmToolchain.ELM_INTELLIJ_JSON]
 */
private class ElmSidecarManifestDTO(
        /**
         * The path to the directory containing the unit tests, relative to the root of the Elm project.
         */
        @JsonProperty("test-directory") val testDirectory: String
)


private class ExactDependenciesDTO(
        @JsonProperty("direct") val direct: Map<String, Version>,
        @JsonProperty("indirect") val indirect: Map<String, Version>
)


private class ElmPackageProjectDTO(
        @JsonProperty("elm-version") val elmVersion: Constraint,
        @JsonProperty("dependencies") val dependencies: Map<String, Constraint>,
        @JsonProperty("test-dependencies") val testDependencies: Map<String, Constraint>,
        @JsonProperty("name") val name: String,
        @JsonProperty("version") val version: Version,
        @JsonProperty("exposed-modules") val exposedModulesNode: JsonNode
) : ElmProjectDTO()


private fun JsonNode.toExposedModuleMap(): List<String> {
    // Normalize the 2 exposed-modules formats into a single format.
    // format 1: a list of strings, where each string is the name of an exposed module
    // format 2: a map where the keys are categories and the values are the names of the modules
    //           exposed in that category. We discard the categories because they are not useful.
    return when (this.nodeType) {
        JsonNodeType.ARRAY -> {
            this.elements().asSequence().map { it.textValue() }.toList()
        }
        JsonNodeType.OBJECT -> {
            this.fields().asSequence().flatMap { (_, nameNodes) ->
                nameNodes.asSequence().map { it.textValue() }
            }.toList()
        }
        else -> {
            throw RuntimeException("exposed-modules JSON must be either an array or an object")
        }
    }
}