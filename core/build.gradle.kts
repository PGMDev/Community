import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName = "Community.jar"
    archiveClassifier.set("")
    destinationDirectory = rootProject.projectDir.resolve("build/libs")

    minimize()

    dependencies {
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
        filesMatching(listOf("plugin.yml")) {
            expand(
                "name" to project.name,
                "description" to project.description,
                "mainClass" to "dev.pgm.community.Community",
                "version" to project.version,
                "commitHash" to project.latestCommitHash(),
                "author" to "applenick",
                "url" to "https://pgm.dev/")
        }
    }

    named("build") {
        dependsOn(shadowJar)
    }
}