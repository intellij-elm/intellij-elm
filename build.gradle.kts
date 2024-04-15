import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.10.0" // Does not work with Gradle 8.6
    // GrammarKit Plugin
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.colormath:colormath:3.4.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.20-M1")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

val generateGrammars = tasks.register("generateGrammars") {
    dependsOn("generateParser", "generateLexer")
}

tasks.withType<KotlinCompile> {
    dependsOn(generateGrammars)
}

tasks {
    sourceSets {
        java.sourceSets["main"].java {
            srcDir("src/main/gen")
        }
    }

    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = it
        }
    }

    generateLexer {
        // ("generateElmLexer") {
        sourceFile.set(file("$projectDir/src/main/grammars/ElmLexer.flex"))
        skeleton.set(file("$projectDir/src/main/grammars/lexer.skeleton"))
        targetOutputDir.set(file("$projectDir/src/main/gen/org/elm/lang/core/lexer/"))
        purgeOldFiles.set(true)
    }

    generateParser {
        //("generateElmParser") {
        sourceFile.set(file("$projectDir/src/main/grammars/ElmParser.bnf"))
        targetRootOutputDir.set(file("$projectDir/src/main/gen"))
        pathToParser.set("/org/elm/lang/core/parser/ElmParser.java")
        pathToPsiRoot.set("/org/elm/lang/core/psi")
        purgeOldFiles.set(true)
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))

        // This prevents the patching `plugin.xml`. as set these manually as `patchPluginXml` can mess it up.
        // See: https://intellij-support.jetbrains.com/hc/en-us/community/posts/360010590059-Why-pluginUntilBuild-is-mandatory
        // Commented out for now as it breaks certain GitHub Workflows
        // intellij.updateSinceUntilBuild.set(false)

        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            changelog.run {
                (getOrNull(properties("pluginVersion")) ?: getUnreleased()).withHeader(false)
            }.toHTML()
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
