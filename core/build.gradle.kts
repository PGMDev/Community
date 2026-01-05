import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly("dev.pgm.paper:paper-api:1.8_1.21.10-SNAPSHOT")

    implementation(project(":util"))

    runtimeOnly(project(":platform-sportpaper")) { exclude("*") }
    runtimeOnly(project(":platform-modern")) { exclude("*") }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName = "Community.jar"
    archiveClassifier.set("")
    destinationDirectory = rootProject.projectDir.resolve("build/libs")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    minimize {
        // Exclude from minimization as they're required at runtime
        exclude(project(":platform-sportpaper"))
        exclude(project(":platform-modern"))
    }

    dependencies {
        exclude(dependency("org.jspecify:jspecify"))
        exclude(dependency("org.jetbrains:annotations"))
    }

    exclude("META-INF/**")
}

publishing {
    publications.create<MavenPublication>("community") {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(tasks["shadowJar"])
    }
    repositories {
        maven {
            name = "ghPackages"
            url = uri("https://maven.pkg.github.com/PGMDev/Community")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks {
    processResources {
        val name = project.name
        val description = project.description
        val version = project.version.toString()
        val commitHash = project.latestCommitHash()

        filesMatching(listOf("plugin.yml")) {
            expand(
                mapOf(
                    "name" to name,
                    "description" to description,
                    "apiVersion" to "1.21.10",
                    "mainClass" to "dev.pgm.community.Community",
                    "version" to version,
                    "commitHash" to commitHash,
                    "author" to "applenick",
                    "url" to "https://pgm.dev/"
                )
            )
        }
    }

    named("build") {
        dependsOn(shadowJar)
    }
}