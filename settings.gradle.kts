//FIXME: remove after release
pluginManagement {
    plugins {
        id("dev.fritz2.fritz2-gradle") version "0.9-SNAPSHOT"
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "fritz2-ktor-todomvc"