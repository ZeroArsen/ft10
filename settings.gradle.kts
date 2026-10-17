pluginManagement {
    plugins {
        // Modrinth publishing (same plugin as rGoldenHeads); applied per version in build.gradle.kts.
        id("com.modrinth.minotaur") version "2.10.0"
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // https://stonecutter.kikugie.dev/blog/changes/0.9
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the right Loom variant per node: remapping Loom for <=1.21.11, plain Loom for unobfuscated 26.1+
    // (https://codeberg.org/KikuGie/loom-back-compat)
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One node per jar. Each node's supported range lives in stonecutter.properties.toml (mod.mc_compat).
        versions("26.2")
        vcsVersion = "26.2"
    }
}

rootProject.name = "ft10"
