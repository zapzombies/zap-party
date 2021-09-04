import io.github.zap.build.gradle.convention.*

// Uncomment to use local maven version - help local testing faster
plugins {
    // id("io.github.zap.build.gradle.convention.shadow-mc-plugin") version "0.0.0-SNAPSHOT"
    id("io.github.zap.build.gradle.convention.shadow-mc-plugin") version "1.0.0"
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    paperApi("1.16.5-R0.1-SNAPSHOT")
    relocate("net.kyori:adventure-text-minimessage:4.1.0-SNAPSHOT") {
        exclude("net.kyori", "adventure-api")
    }
    relocate("com.github.Steanky:RegularCommands:master-SNAPSHOT")
    relocate("org.apache.commons:commons-lang3:3.12.0")
}

publishToZGpr()