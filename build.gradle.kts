plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/creatorfromhell/")
    flatDir {
        dir("libs")
    }
}

dependencies {
    compileOnly(files("libs/ProfilesExtension.jar"))
}

group = "btc.renaud"
version = "0.0.2"


typewriter {
    namespace = "renaud"

    extension {
        name = "NumericalStorage"
        shortDescription = "Create a Bank System in TypeWriter"
        description = """
            NumericalStorage is a TypeWriter extension that provides a comprehensive numerical storage system.
            NumericalStorage is a TypeWriter extension that provides a comprehensive numerical storage system.
        """.trimIndent()
        engineVersion = "0.9.0-beta-171"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA

        dependencies {
            paper()
        }
    }
}

kotlin {
    jvmToolchain(21)
}

