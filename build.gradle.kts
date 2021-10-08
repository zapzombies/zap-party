import io.github.zap.build.gradle.convention.paperApi
import io.github.zap.build.gradle.convention.publishToZGpr
import io.github.zap.build.gradle.convention.zgpr

// Uncomment to use local maven version - help local testing faster
plugins {
    // id("io.github.zap.build.gradle.convention.shadow-mc-plugin") version "0.0.0-SNAPSHOT"
    id("io.github.zap.build.gradle.convention.shadow-mc-plugin") version "1.0.0"
}

repositories {
    maven(zgpr("regular-commands"))
}

dependencies {
    paperApi("1.16.5-R0.1-SNAPSHOT")
    relocate("net.kyori:adventure-text-minimessage:4.1.0-SNAPSHOT") {
        exclude("net.kyori", "adventure-api")
    }
    relocate("io.github.zap:regular-commands:1.0.0")
    relocate("org.apache.commons:commons-lang3:3.12.0")
    relocate("com.ibm.icu:icu4j:69.1")
}

publishToZGpr()
