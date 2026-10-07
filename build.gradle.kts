plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.2.0"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.typewritermc.com/beta/")
    maven("https://maven.typewritermc.com/external")
    maven("https://repo.codemc.io/repository/creatorfromhell")
    // MMOProfiles API (Phoenix Development): compile-only, detected at runtime.
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
}

dependencies {
    compileOnly(project(":Typewriter-OmniGUIExtension"))
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")
    // Profile mode. No `paper { dependency(...) }`: that would make MMOProfiles required, and the extension only keys
    // balances by profile when the plugin is there. Its API is read through its Bukkit service.
    compileOnly("fr.phoenixdevt:Profile-API:1.2.1")
    testImplementation("fr.phoenixdevt:Profile-API:1.2.1")
    // The API's interfaces mention Bukkit types (Player, Location, ItemStack): the stand-ins of the tests need them.
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
}

group = "btc.renaud"
version = "0.14"

base {
    archivesName.set("NumericalStorageExtension")
}

typewriter {
    namespace = "btcrenaud"
    extension {
        name = "NumericalStorage"
        shortDescription = "Create a Bank System in TypeWriter"
        description = "A comprehensive TypeWriter extension providing advanced gameplay features for Minecraft servers on Paper 1.21+. Fully compatible with the official TypeWriter engine and PlaceholderAPI."
        engineVersion = "0.9.0-beta-177"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        
        dependencies {
            dependency(namespace = "renaud", name = "GuiAndDialogs")
        }
        paper()
    }
}

kotlin {
    jvmToolchain(21)
}

