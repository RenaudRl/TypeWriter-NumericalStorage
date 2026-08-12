pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.typewritermc.com/beta")
    }
}

rootProject.name = "NumericalStorageExtension"

include("Typewriter-OmniGUIExtension")
project(":Typewriter-OmniGUIExtension").projectDir = file("../Typewriter-OmniGUIExtension")

