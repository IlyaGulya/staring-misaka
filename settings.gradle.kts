
import java.util.*
import kotlin.io.encoding.ExperimentalEncodingApi

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.vaadin.com/vaadin-prereleases")
        maven("https://repo.spring.io/milestone")
        mavenCentral()
    }
    plugins {
        val vaadinVersion: String by settings
        id("com.vaadin") version vaadinVersion
    }
}

@OptIn(ExperimentalEncodingApi::class)
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.vaadin.com/vaadin-prereleases")
        maven("https://repo.spring.io/milestone")
        maven("https://maven.vaadin.com/vaadin-addons")
        maven("https://maven.pkg.github.com/p-vorobyev/spring-boot-starter-telegram") {
            credentials {
                val properties = Properties().apply {
                    load(File(rootProject.projectDir, "local.properties").inputStream())
                }
                username = properties.getProperty("github.username")
                password = properties.getProperty("github.token")
            }
        }
    }
}