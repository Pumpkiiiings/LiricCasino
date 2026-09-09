import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.yaml.snakeyaml.Yaml

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:9.6")
        classpath("org.ow2.asm:asm-commons:9.6")
        classpath("org.yaml:snakeyaml:2.2")
    }
}

plugins {
    java
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "liric.mistaken" // Actualizado a tu nuevo package
version = "2.0.4"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.triumphteam.dev/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("dev.triumphteam:triumph-gui:3.1.13")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")

    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")

    // Paper ya incluye Adventure y MiniMessage nativamente
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
        isZip64 = true
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // RELOCACIONES: Para que no haya conflicto con otros plugins
        relocate("dev.triumphteam.gui", "liric.mistaken.libs.gui")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn("verifyPluginJar", "verifyYamlResources")
    }
}

tasks.register("verifyYamlResources") {
    val yamlFiles = fileTree("src/main/resources") { include("**/*.yml") }
    inputs.files(yamlFiles)
    doLast {
        yamlFiles.forEach { file ->
            file.inputStream().use { Yaml().load<Any?>(it) }
        }
    }
}

tasks.register("verifyPluginJar") {
    dependsOn(tasks.shadowJar)
    doLast {
        val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
        val entries = zipTree(jarFile)
        val descriptor = entries.matching { include("plugin.yml") }.singleFile
        check(descriptor.readText().isNotBlank()) { "plugin.yml is missing or empty in ${jarFile.name}" }
        check(!entries.matching { include("org/sqlite/JDBC.class") }.isEmpty) {
            "SQLite JDBC driver is missing from ${jarFile.name}"
        }
        check(!entries.matching { include("org/mariadb/jdbc/Driver.class") }.isEmpty) {
            "MariaDB JDBC driver is missing from ${jarFile.name}"
        }
    }
}
