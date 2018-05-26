package org.elm.workspace

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.intellij.openapi.vfs.VirtualFile
import org.elm.openapiext.CachedVirtualFile
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
        val elmVersion: String,
        val dependencies: List<ElmPackageRef>,
        val testDependencies: List<ElmPackageRef>,
        val transitiveDependencies: List<ElmPackageRef>
) {

    /**
     * Returns the `elm.json` file which defines this project.
     */
    val manifestFile: VirtualFile? by CachedVirtualFile(manifestPath.toUri().toString())

    private val projectDirPath get() =
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
        get() = sequenceOf(dependencies, testDependencies, transitiveDependencies).flatten()


    companion object {
        /**
         * Attempts to parse an `elm.json` file
         *
         * @throws ProjectLoadException if the JSON cannot be parsed
         */
        @Throws(ProjectLoadException::class)
        fun parse(inputStream: InputStream, manifestPath: Path, toolchain: ElmToolchain): ElmProject {
            val node = try {
                objectMapper.readTree(inputStream)
            } catch (e: JsonProcessingException) {
                throw ProjectLoadException("Bad JSON: ${e.message}")
            }
            val type = node.get("type")?.textValue()
            return when (type) {
                "application" -> {
                    val dto = objectMapper.treeToValue(node, ElmApplicationProjectDTO::class.java)
                    ElmApplicationProject(
                            manifestPath = manifestPath,
                            elmVersion = dto.elmVersion,
                            dependencies = dto.dependencies.depsToPackages(toolchain),
                            testDependencies = dto.testDependencies.depsToPackages(toolchain),
                            sourceDirectories = dto.sourceDirectories,
                            transitiveDependencies = dto.doNotEditThisByHand.transitiveDependencies
                                    .depsToPackages(toolchain)
                    )
                }
                "package" -> {
                    throw ProjectLoadException("Elm 'package' projects are not currently supported. "
                            + "As a workaround, please add your example app project instead.")
                    val dto = objectMapper.treeToValue(node, ElmPackageProjectDTO::class.java)
                    // TODO [kl] fix the resolving of dependencies:
                    //           - resolve version constraint to a specific version
                    //           - do not use the empty list for transitiveDependencies
                    ElmPackageProject(
                            manifestPath = manifestPath,
                            elmVersion = dto.elmVersion,
                            dependencies = dto.dependencies.depsToPackages(toolchain),
                            testDependencies = dto.testDependencies.depsToPackages(toolchain),
                            transitiveDependencies = emptyList(),
                            name = dto.name,
                            summary = dto.summary,
                            version = dto.version,
                            license = dto.license,
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
        elmVersion: String,
        dependencies: List<ElmPackageRef>,
        testDependencies: List<ElmPackageRef>,
        transitiveDependencies: List<ElmPackageRef>,
        val sourceDirectories: List<String>
): ElmProject(manifestPath, elmVersion, dependencies, testDependencies, transitiveDependencies)


/**
 * Represents an Elm package/library
 */
class ElmPackageProject(
        manifestPath: Path,
        elmVersion: String,
        dependencies: List<ElmPackageRef>,
        testDependencies: List<ElmPackageRef>,
        transitiveDependencies: List<ElmPackageRef>,
        val name: String,
        val summary: String,
        val license: String,
        val version: String,
        /** Map from label to one-or-more module names. The label can be the empty string. */
        val exposedModules: Map<String, List<String>>
): ElmProject(manifestPath, elmVersion, dependencies, testDependencies, transitiveDependencies)


/**
 * A dependency reference to an Elm package
 */
class ElmPackageRef(
        val root: VirtualFile?,
        val name: String,
        val version: String
)


private fun Map<String, String>.depsToPackages(toolchain: ElmToolchain) =
        map { (name, version) ->
            ElmPackageRef(
                root = toolchain.packageRootDir(name, version),
                name = name,
                version = version) }


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
        elmVersion = "",
        dependencies = emptyList(),
        testDependencies = emptyList(),
        sourceDirectories = emptyList(),
        transitiveDependencies = emptyList()
)



// JSON Decoding


@JsonIgnoreProperties(ignoreUnknown = true)
private interface ElmProjectDTO


private class ElmApplicationProjectDTO(
        @JsonProperty("elm-version")                val elmVersion: String,
        @JsonProperty("dependencies")               val dependencies: Map<String, String>,
        @JsonProperty("test-dependencies")          val testDependencies: Map<String, String>,
        @JsonProperty("source-directories")         val sourceDirectories: List<String>,
        @JsonProperty("do-not-edit-this-by-hand")   val doNotEditThisByHand: DoNotEditThisByHandDTO
): ElmProjectDTO


private class DoNotEditThisByHandDTO(
        @JsonProperty("transitive-dependencies") val transitiveDependencies: Map<String, String>
)


private class ElmPackageProjectDTO(
        @JsonProperty("elm-version")        val elmVersion: String,
        @JsonProperty("dependencies")       val dependencies: Map<String, String>,
        @JsonProperty("test-dependencies")  val testDependencies: Map<String, String>,
        @JsonProperty("name")               val name: String,
        @JsonProperty("summary")            val summary: String,
        @JsonProperty("license")            val license: String,
        @JsonProperty("version")            val version: String,
        @JsonProperty("exposed-modules")    val exposedModulesNode: JsonNode // either List<String>
                                                                             // or Map<String, List<String>>
                                                                             // where the map's keys are labels
): ElmProjectDTO
