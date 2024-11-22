plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("de.skuzzle.restrictimports")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.pgm.fyi/snapshots") // Sportpaper & other pgm-specific stuff
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot repo
    maven("https://repo.aikar.co/content/groups/aikar/") // aikar repo
}

dependencies {
    implementation("tc.oc.pgm:util:0.16-SNAPSHOT") { isTransitive = false }
    implementation("com.zaxxer:HikariCP:2.4.1") { isTransitive = false }
    implementation("fr.minuskube.inv:smart-invs:1.2.7") { isTransitive = false }

    implementation("redis.clients:jedis:3.5.1")
    implementation("co.aikar:idb-core:1.0.0-SNAPSHOT")
    implementation("co.aikar:idb-bukkit:1.0.0-SNAPSHOT")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.17.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")

    compileOnly("app.ashcon:sportpaper:1.8.8-R0.1-SNAPSHOT")
    compileOnly("tc.oc.pgm:core:0.16-SNAPSHOT")
    compileOnly("tc.oc.occ:AFK:1.0.0-SNAPSHOT")
    compileOnly("tc.oc.occ:Environment:1.0.0-SNAPSHOT")
}

group = "dev.pgm"
version = "0.2-SNAPSHOT"
description = "A plugin for managing a Minecraft community"

tasks {
    withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }
    withType<Javadoc>() {
        options.encoding = "UTF-8"
    }
}

spotless {
    ratchetFrom = "origin/dev"
    java {
        removeUnusedImports()
        palantirJavaFormat("2.47.0").style("GOOGLE").formatJavadoc(true)
    }
}

restrictImports {
    group {
        reason = "Use org.jetbrains.annotations to add annotations"
        bannedImports = listOf("javax.annotation.**")
    }
    group {
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
}