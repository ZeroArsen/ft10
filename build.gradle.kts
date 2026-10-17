plugins {
    // Picks remapping Loom for obfuscated versions (<=1.21.11) and plain Loom for 26.1+.
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...! (Stonecutter nodes share one group; see the template notes.)
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

repositories {
    /** Restricts lookups of [groups] to one maven so dependency resolution stays fast. */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang mappings on obfuscated versions, a no-op on 26.1+ (already unobfuscated).
    loomx.applyMojangMappings()

    // Use mod* configurations everywhere; loom-back-compat converts them on 26.1+.
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("maven.modrinth:yacl:${property("deps.yacl")}")
    modCompileOnly("maven.modrinth:modmenu:${property("deps.modmenu")}")
    modLocalRuntime("maven.modrinth:modmenu:${property("deps.modmenu")}")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // names lambdas, handy when writing mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // shared between versions
        jvmArguments.add("-Dmixin.debug.export=true")
    }

    // `-Pft10.join=localhost` makes both dev clients join that server straight away (see context/TESTING.md).
    val joinServer = providers.gradleProperty("ft10.join").orNull

    runs {
        named("client") {
            programArgs("--username", "Ft10Dev")
            if (joinServer != null) programArgs("--quickPlayMultiplayer", joinServer)
        }
        // A second client so two players can fight each other on the local dev server.
        register("opponent") {
            client()
            programArgs("--username", "Ft10Rival")
            if (joinServer != null) programArgs("--quickPlayMultiplayer", joinServer)
        }
    }
}

// Keep each run in its own folder so two clients and the server don't fight over logs/options.
afterEvaluate {
    loom.runConfigs.named("opponent") { runDirectory = rootProject.file("run/opponent") }
    loom.runConfigs.named("server") { runDirectory = rootProject.file("run/server") }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            register("loader", Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    test {
        useJUnitPlatform()
    }

    // Ships the license inside the jar.
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies them to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
