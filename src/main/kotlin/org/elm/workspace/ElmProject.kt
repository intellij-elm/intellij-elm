package org.elm.workspace

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.intellij.openapi.vfs.VirtualFile
import org.elm.openapiext.CachedVirtualFile
import org.elm.workspace.ElmToolchain.Companion.ELM_LEGACY_JSON
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths


private val objectMapper = ObjectMapper()

/**
 * The logical representation of an Elm project. An Elm project can be an application
 * or a package, and it specifies its dependencies.
 */
sealed class ElmProject(
        val manifestPath: Path,
        val dependencies: List<ElmPackageRef>,
        val testDependencies: List<ElmPackageRef>
) {

    /**
     * Returns the `elm.json` file which defines this project.
     */
    val manifestFile: VirtualFile? by CachedVirtualFile(manifestPath.toUri().toString())


    private val projectDirPath
        get() =
            manifestPath.parent


    /**
     * A name which can be shown in the UI. Note that while Elm packages have user-assigned
     * names, applications do not. Thus, in order to cover both cases, we use the name
     * of the parent directory.
     */
    val presentableName: String
        get() = projectDirPath.fileName.toString()


    /**
     * The directory containing the project.
     */
    val projectDir: VirtualFile? by CachedVirtualFile(projectDirPath.toUri()?.toString())


    /**
     * Returns all packages which this project depends on, whether it be for normal,
     * production code or for tests.
     */
    val allResolvedDependencies: Sequence<ElmPackageRef>
        get() = sequenceOf(dependencies, testDependencies).flatten()


    companion object {
        /**
         * Attempts to parse an `elm.json` file
         *
         * @throws ProjectLoadException if the JSON cannot be parsed
         */
        @Throws(ProjectLoadException::class)
        fun parse(inputStream: InputStream, manifestPath: Path, toolchain: ElmToolchain): ElmProject {

            if (manifestPath.endsWith(ELM_LEGACY_JSON)) {
                // Handle legacy Elm 0.18 package. We don't need to model the dependencies
                // because Elm 0.18 stored everything in a local `elm-stuff` directory, and we assume
                // that the user has not excluded that directory from the project.
                return ElmApplicationProject(
                        manifestPath = manifestPath,
                        elmVersion = Version(0, 18, 0),
                        dependencies = emptyList(),
                        testDependencies = emptyList(),
                        sourceDirectories = emptyList()
                )
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
                            dependencies = dto.dependencies.toPackageRefs(toolchain),
                            testDependencies = dto.testDependencies.toPackageRefs(toolchain),
                            sourceDirectories = dto.sourceDirectories
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
                            dependencies = dto.dependencies.constraintDepsToPackages(toolchain),
                            testDependencies = dto.testDependencies.constraintDepsToPackages(toolchain),
                            name = dto.name,
                            version = dto.version,
                            exposedModules = dto.exposedModulesNode.toExposedModuleMap())
                }
                else -> throw ProjectLoadException("The 'type' field is '$type', "
                        + "but expected either 'application' or 'package'")
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
        dependencies: List<ElmPackageRef>,
        testDependencies: List<ElmPackageRef>,
        val sourceDirectories: List<String>
) : ElmProject(manifestPath, dependencies, testDependencies)


/**
 * Represents an Elm package/library
 */
class ElmPackageProject(
        manifestPath: Path,
        val elmVersion: Constraint,
        dependencies: List<ElmPackageRef>,
        testDependencies: List<ElmPackageRef>,
        val name: String,
        val version: Version,
        /** Map from label to one-or-more module names. The label can be the empty string. */
        val exposedModules: Map<String, List<String>>
) : ElmProject(manifestPath, dependencies, testDependencies)


/**
 * A dependency reference to an Elm package
 */
class ElmPackageRef(
        val root: VirtualFile?,
        val name: String,
        val version: Version
)


private fun ExactDependenciesDTO.toPackageRefs(toolchain: ElmToolchain) =
        direct.depsToPackages(toolchain) + indirect.depsToPackages(toolchain)


private fun Map<String, Version>.depsToPackages(toolchain: ElmToolchain) =
        map { (name, version) ->
            ElmPackageRef(
                    root = toolchain.packageVersionDir(name, version),
                    name = name,
                    version = version)
        }

private fun Map<String, Constraint>.constraintDepsToPackages(toolchain: ElmToolchain) =
        map { (name, constraint) ->
            val useVersion = toolchain.availableVersionsForPackage(name)
                    .filter { constraint.contains(it) }
                    .sorted()
                    .first()
            ElmPackageRef(
                    root = toolchain.packageVersionDir(name, useVersion),
                    name = name,
                    version = useVersion)
        }


private fun JsonNode.toExposedModuleMap(): Map<String, List<String>> {
    // normalize the 2 exposed-modules formats into a single format
    return when (this.nodeType) {
        JsonNodeType.ARRAY -> {
            val moduleNames = this.elements().asSequence().map { it.textValue() }.toList()
            mapOf("" to moduleNames)
        }
        JsonNodeType.OBJECT -> {
            this.fields().asSequence().map { (label, nameNodes) ->
                label to nameNodes.asSequence().map { it.textValue() }.toList()
            }.toMap()
        }
        else -> {
            throw RuntimeException("exposed-modules JSON must be either an array or an object")
        }
    }
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
        @JsonProperty("source-directories") val sourceDirectories: List<String>,
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
        @JsonProperty("exposed-modules") val exposedModulesNode: JsonNode // either List<String>
        // or Map<String, List<String>>
        // where the map's keys are labels
) : ElmProjectDTO


