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
    mavenCentral()
    maven("https://repo.pgm.fyi/snapshots") // SportPaper & other PGM-specific stuff
    maven("https://repo.papermc.io/repository/maven-public/") // Paper builds & paperweight plugin
    maven("https://repo.aikar.co/content/groups/aikar/") // Aikar repo
    maven("https://repo.codemc.io/repository/maven-releases/") // PacketEvents
    exclusiveContent {
        forRepository {
            maven("https://jitpack.io")
        }
        filter {
            includeGroup("com.github.OvercastCommunity.adventure-platform")
            includeGroup("com.github.MinusKube")
        }
    }
    mavenLocal() // Local last
}

dependencies {
    api("com.zaxxer:HikariCP:2.4.1") { isTransitive = false }
    // Latest SmartInvs commit
    api("com.github.MinusKube:SmartInvs:9c9dbbee16") { isTransitive = false }
    api("redis.clients:jedis:3.5.1")
    api("net.kyori:adventure-api:4.26.1")
    api("net.kyori:adventure-text-serializer-plain:4.26.1")
    // adventure-platform fork with ViaVersion and 1.21.11+ fixes
    // https://github.com/OvercastCommunity/adventure-platform
    api("com.github.OvercastCommunity.adventure-platform:adventure-platform-bukkit:04de657e85")
    api("org.reflections:reflections:0.10.2")

    // Annotations
    api("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // Runtime dependencies
    compileOnly("tc.oc.pgm:core:0.16-SNAPSHOT")
    compileOnly("tc.oc.pgm:util:0.16-SNAPSHOT")
    compileOnly("tc.oc.occ:Environment:1.0.0-SNAPSHOT")
    compileOnly("org.incendo:cloud-annotations:2.0.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0")

    // Paper and SportPaper include these (or equivalents)
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("com.google.guava:guava:17.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.apache.commons:commons-lang3:3.17.0")
}

group = "dev.pgm.community"
version = "0.2-SNAPSHOT"
description = "A plugin for managing a Minecraft community"

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}

spotless {
    ratchetFrom = "origin/dev"
    java {
        removeUnusedImports()
        trimTrailingWhitespace()
        formatAnnotations()
        palantirJavaFormat("2.90.0").style("GOOGLE").formatJavadoc(true)
    }
}

restrictImports {
    group {
        reason = "Use org.jspecify.annotations to add annotations, or org.jetbrains.annotations if needed"
        bannedImports = listOf("javax.annotation.**")
    }
    group {
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
}
