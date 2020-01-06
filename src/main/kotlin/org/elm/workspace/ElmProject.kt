package org.elm.workspace

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.LocalFileSystem
import org.elm.workspace.ElmToolchain.Companion.ELM_LEGACY_JSON
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths


private val objectMapper = ObjectMapper()

/**
 * The logical representation of an Elm project. An Elm project can be an application
 * or a package, and it specifies its dependencies.
 *
 * @param manifestPath The location of the manifest file (e.g. `elm.json`). Uniquely identifies a project.
 * @param dependencies Additional Elm packages that this project depends on
 * @param testDependencies Additional Elm packages that this project's **tests** depends on
 * @param sourceDirectories The relative paths to one-or-more directories containing Elm source files belonging to this project.
 * @param testsRelativeDirPath The path, relative to the [projectDirPath], to the directory containing unit tests.
 * Typically this will be "tests": see [testsDirPath] for more info.
 */
sealed class ElmProject(
        val manifestPath: Path,
        val dependencies: List<ElmPackageProject>,
        val testDependencies: List<ElmPackageProject>,
        val sourceDirectories: List<Path>,
        testsRelativeDirPath: String
) : UserDataHolderBase() {

    /**
     * The path to the directory containing the Elm project JSON file.
     */
    val projectDirPath: Path = manifestPath.parent

    /**
     * The path to the directory containing unit tests.
     *
     * For packages this will be a directory called "tests", as elm-test requires packages to have tests in a top-level
     * "tests" directory. For applications the default behaviour is the same as for packages, but optionally tests can
     * be put in some other directory, as long as when elm-test is called, the path to those tests is specified as a
     * cmd-line argument.
     *
     * This path is already normalized (see [Path.normalize]) so this doesn't need to be done be done when comparing this
     * to other paths.
     */
    val testsDirPath: Path = projectDirPath.resolve(testsRelativeDirPath).normalize()

    /**
     * A name which can be shown in the UI. Note that while Elm packages have user-assigned
     * names, applications do not. Thus, in order to cover both cases, we use the name
     * of the parent directory.
     */
    val presentableName: String =
            projectDirPath.fileName?.toString() ?: "UNKNOWN"

    /**
     * Returns the absolute paths for each source directory.
     *
     * @see sourceDirectories
     */
    val absoluteSourceDirectories: List<Path> =
            sourceDirectories.map { projectDirPath.resolve(it).normalize() }

    /**
     * Returns all the source directories, i.e. the [absoluteSourceDirectories] and the [testsDirPath].
     */
    val allSourceDirs: Sequence<Path> =
            absoluteSourceDirectories.asSequence() + sequenceOf(testsDirPath)

    /**
     * Returns all packages which this project depends on, whether it be for normal,
     * production code or for tests.
     */
    val allResolvedDependencies: Sequence<ElmPackageProject> =
            sequenceOf(dependencies, testDependencies).flatten()

    val isElm18: Boolean
        get() = when (this) {
            is ElmApplicationProject -> elmVersion == Version.ELM_18
            is ElmPackageProject -> elmVersion.contains(Version.ELM_18)
        }

    /**
     * Returns true if this project is compatible with Elm compiler [version].
     *
     * This is a looser form of a version check that allows for Elm compiler versions that include
     * alpha/beta/rc suffixes. e.g. "0.19.1-alpha-4"
     */
    fun isCompatibleWith(version: Version) =
            when (this) {
                is ElmApplicationProject -> elmVersion.xyz == version.xyz
                is ElmPackageProject -> elmVersion.contains(version.xyz)
            }

    companion object {

        fun parse(manifestPath: Path, repo: ElmPackageRepository, ignoreTestDeps: Boolean = false): ElmProject {
            val inputStream = LocalFileSystem.getInstance().refreshAndFindFileByPath(manifestPath.toString())?.inputStream
                    ?: throw ProjectLoadException("Could not find file $manifestPath. Is the package installed?")
            return parse(inputStream, manifestPath, repo, ignoreTestDeps)
        }

        /**
         * Attempts to parse an `elm.json` file.
         *
         * @throws ProjectLoadException if the JSON cannot be parsed
         */
        fun parse(inputStream: InputStream, manifestPath: Path, repo: ElmPackageRepository, ignoreTestDeps: Boolean = false): ElmProject {

            if (manifestPath.endsWith(ELM_LEGACY_JSON)) {
                val elmStuffPath = manifestPath.resolveSibling("elm-stuff")
                return parseLegacy(manifestPath, elmStuffPath)
            }

            val node = try {
                objectMapper.readTree(inputStream)
            } catch (e: JsonProcessingException) {
                throw ProjectLoadException("Bad JSON: ${e.message}")
            }

            val type = node.get("type")?.textValue()
            return when (type) {
                "application" -> {
                    val dto = try {
                        objectMapper.treeToValue(node, ElmApplicationProjectDTO::class.java)
                    } catch (e: JsonProcessingException) {
                        throw ProjectLoadException("Invalid elm.json: ${e.message}")
                    }
                    ElmApplicationProject(
                            manifestPath = manifestPath,
                            elmVersion = dto.elmVersion,
                            dependencies = dto.dependencies.depsToPackages(repo),
                            testDependencies = if (ignoreTestDeps) emptyList() else dto.testDependencies.depsToPackages(repo),
                            sourceDirectories = dto.sourceDirectories
                            // TODO [tests-folder]: allow a value for testsRelativeDirPath to be specified, by reading from some config file.
                    )
                }
                "package" -> {
                    val dto = try {
                        objectMapper.treeToValue(node, ElmPackageProjectDTO::class.java)
                    } catch (e: JsonProcessingException) {
                        throw ProjectLoadException("Invalid elm.json: ${e.message}")
                    }
                    // TODO [kl] resolve dependency constraints to determine package version numbers
                    // [x] use whichever version number is available in the Elm package cache (~/.elm)
                    // [ ] include transitive dependencies
                    // [ ] resolve versions such that all constraints are satisfied
                    //     (necessary for correctness sake, but low priority)
                    ElmPackageProject(
                            manifestPath = manifestPath,
                            elmVersion = dto.elmVersion,
                            dependencies = dto.dependencies.constraintDepsToPackages(repo),
                            testDependencies = if (ignoreTestDeps) emptyList() else dto.testDependencies.constraintDepsToPackages(repo),
                            sourceDirectories = listOf(Paths.get("src")),
                            name = dto.name,
                            version = dto.version,
                            exposedModules = dto.exposedModulesNode.toExposedModuleMap())
                }
                else -> throw ProjectLoadException("The 'type' field is '$type', "
                        + "but expected either 'application' or 'package'")
            }
        }

        // TODO [drop 0.18]
        fun parseLegacy(manifestPath: Path, elmStuffPath: Path): ElmProject {
            // Handle legacy Elm 0.18 projects
            val dto = try {
                objectMapper.readValue(File(manifestPath.toString()), Elm18ProjectDTO::class.java)
            } catch (e: JsonProcessingException) {
                throw ProjectLoadException("Invalid elm-package.json: ${e.message}")
            }

            return if (dto.exposedModules.isEmpty()) {
                ElmApplicationProject(
                        manifestPath = manifestPath,
                        elmVersion = Version(0, 18, 0),
                        dependencies = dto.depsToPackages(elmStuffPath),
                        testDependencies = emptyList(),
                        sourceDirectories = dto.sourceDirectories)
            } else {
                ElmPackageProject(
                        manifestPath = manifestPath,
                        elmVersion = dto.elmVersion,
                        dependencies = dto.depsToPackages(elmStuffPath),
                        testDependencies = emptyList(),
                        name = if (manifestPath.startsWith(elmStuffPath)) {
                            // convert "<absolute_path>/foo/elm-stuff/packages/elm-lang/html/1.0.0/elm-package.json"
                            // to "elm-lang/html"
                            elmStuffPath.relativize(manifestPath).drop(1).take(2).joinToString("/")
                        } else {
                            manifestPath.parent.fileName.toString()
                        },
                        version = dto.version,
                        exposedModules = dto.exposedModules,
                        sourceDirectories = dto.sourceDirectories)
            }
        }
    }
}


/**
 * Represents an Elm application
 */
class ElmApplicationProject(
        manifestPath: Path,
        val elmVersion: Version,
        dependencies: List<ElmPackageProject>,
        testDependencies: List<ElmPackageProject>,
        sourceDirectories: List<Path>,
        testsRelativeDirPath: String = DEFAULT_TESTS_DIR_NAME
) : ElmProject(manifestPath, dependencies, testDependencies, sourceDirectories, testsRelativeDirPath)


/**
 * Represents an Elm package/library
 */
class ElmPackageProject(
        manifestPath: Path,
        val elmVersion: Constraint,
        dependencies: List<ElmPackageProject>,
        testDependencies: List<ElmPackageProject>,
        sourceDirectories: List<Path>,
        val name: String,
        val version: Version,
        val exposedModules: List<String>
) : ElmProject(manifestPath, dependencies, testDependencies, sourceDirectories, DEFAULT_TESTS_DIR_NAME)


private fun ExactDependenciesDTO.depsToPackages(repo: ElmPackageRepository) =
        direct.depsToPackages(repo) + indirect.depsToPackages(repo)


private fun Map<String, Version>.depsToPackages(repo: ElmPackageRepository) =
        map { (name, version) ->
            loadDependency(repo, name, version)
        }

private fun Map<String, Constraint>.constraintDepsToPackages(repo: ElmPackageRepository) =
        map { (name, constraint) ->
            val version = repo.availableVersionsForPackage(name)
                    .filter { constraint.contains(it) }
                    .min()
                    ?: throw ProjectLoadException("Could not load $name ($constraint). Is it installed?")

            loadDependency(repo, name, version)
        }

private fun loadDependency(repo: ElmPackageRepository, name: String, version: Version): ElmPackageProject {
    val manifestPath = repo.findPackageManifest(name, version)
            ?: throw ProjectLoadException("Could not load $name ($version): manifest not found")
    // TODO [kl] guard against circular dependencies
    // NOTE: we ignore the test dependencies of our dependencies because it is highly unlikely
    // that they have been installed by Elm in the local package cache (the user would have
    // to actually run the package's tests from within the package cache, which no one is going to do).
    val elmProject = ElmProject.parse(manifestPath, repo, ignoreTestDeps = true) as? ElmPackageProject
            ?: throw ProjectLoadException("Could not load $name ($version): expected a package!")

    return elmProject
}


// TODO [drop 0.18] remove me
private fun loadPackageLegacy(elmStuffPath: Path, name: String, version: Version): ElmPackageProject {
    val manifestPath =
            elmStuffPath.resolve("packages")
                    .resolve(name)
                    .resolve(version.toString())
                    .resolve("elm-package.json")

    return ElmProject.parseLegacy(manifestPath, elmStuffPath) as? ElmPackageProject
            ?: throw ProjectLoadException("Could not load $name ($version): expected an Elm 0.18 package!")
}


/**
 * A dummy sentinel value because [LightDirectoryIndex] needs it.
 */
val noProjectSentinel = ElmApplicationProject(
        manifestPath = Paths.get("/elm.json"),
        elmVersion = Version(0, 0, 0),
        dependencies = emptyList(),
        testDependencies = emptyList(),
        sourceDirectories = emptyList()
)


// JSON Decoding


@JsonIgnoreProperties(ignoreUnknown = true)
private interface ElmProjectDTO


private class ElmApplicationProjectDTO(
        @JsonProperty("elm-version") val elmVersion: Version,
        @JsonProperty("source-directories") val sourceDirectories: List<Path>,
        @JsonProperty("dependencies") val dependencies: ExactDependenciesDTO,
        @JsonProperty("test-dependencies") val testDependencies: ExactDependenciesDTO
) : ElmProjectDTO


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
) : ElmProjectDTO


// TODO [drop 0.18]
private class Elm18ProjectDTO(
        @JsonProperty("elm-version") val elmVersion: Constraint,
        @JsonProperty("source-directories") val sourceDirectories: List<Path>,
        @JsonProperty("dependencies") val dependencies: Map<String, Constraint>,
        @JsonProperty("version") val version: Version,
        @JsonProperty("exposed-modules") val exposedModules: List<String>
) : ElmProjectDTO {

    object VERSION_MAP_TYPEREF : TypeReference<Map<String, Version>>()

    fun depsToPackages(elmStuffPath: Path): List<ElmPackageProject> {
        val exactDepsFile = File(elmStuffPath.resolve("exact-dependencies.json").toString())
        val exactDepsMap: Map<String, Version> = try {
            objectMapper.readValue(exactDepsFile, VERSION_MAP_TYPEREF)
        } catch (e: JsonProcessingException) {
            throw ProjectLoadException("Invalid exact-dependencies.json: ${e.message}")
        } catch (e: FileNotFoundException) {
            throw ProjectLoadException("Could not find exact-dependencies.json. Did you run `elm-package install`?")
        }

        return dependencies.keys.map {
            val exactVersion = exactDepsMap[it]
                    ?: throw ProjectLoadException("Could not find exact version of $it")
            loadPackageLegacy(elmStuffPath, it, exactVersion)
        }
    }
}


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

/**
 * The default name of the directory which contains unit tests.
 */
const val DEFAULT_TESTS_DIR_NAME = "tests"