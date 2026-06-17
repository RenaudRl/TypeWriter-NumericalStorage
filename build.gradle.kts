plugins {
    kotlin("jvm") version "2.3.20"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.typewritermc.com/beta/")
    maven("https://maven.typewritermc.com/external")
    maven("https://repo.codemc.io/repository/creatorfromhell")
    mavenLocal()
}

dependencies {
    compileOnly(files("/../shared-libs/Typewriter-GUIExtension-0.0.1.jar"))
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")
}

group = "btc.renaud"
version = "0.0.1"

dependencies {
    implementation(files("../Typewriter-GUIExtension/build/libs/Typewriter-GUIExtension-0.1.0.jar"))
}

typewriter {
    namespace = "btcrenaud"
    extension {
        name = "NumericalStorage"
        shortDescription = "Create a Bank System in TypeWriter"
        description = "A comprehensive TypeWriter extension providing advanced gameplay features for Minecraft servers on Paper 1.21+. Fully compatible with the official TypeWriter engine and PlaceholderAPI."
        engineVersion = "0.9.0-beta-174"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        
        paper()
    }
}

    

kotlin {
    jvmToolchain(25)
    
}
