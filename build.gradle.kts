import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij.platform") version "2.1.0"
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
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(properties("platformVersion"))

        plugins(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))

        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("com.github.ajalt.colormath:colormath:2.1.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
    }
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

// This was needed on the `clojj-master` branch for the tests to pass
tasks.withType<Test> {
    jvmArgs(
        "--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.ref=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        // "--add-opens=java.base/java.security=ALL-UNNAMED", // saw this once, did not see it again
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
        "--add-opens=java.desktop/java.beans=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.font=ALL-UNNAMED"
    )
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

    wrapper {
        gradleVersion = properties("gradleVersion")
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

    patchPluginXml {
        version = properties("pluginVersion")

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

intellijPlatformTesting {
    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
