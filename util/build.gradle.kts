plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
}

dependencies {
    compileOnly("dev.pgm.paper:paper-api:1.8_1.21.10-SNAPSHOT")
}

publishing {
    publications.create<MavenPublication>("community") {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(tasks["jar"])
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