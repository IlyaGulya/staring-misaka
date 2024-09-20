plugins {
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    id("com.vaadin")
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.0.20"
    kotlin("plugin.jpa") version "2.0.20"
}

configurations {
    developmentOnly
    runtimeClasspath {
        extendsFrom(configurations.getByName("developmentOnly"))
    }
}

dependencies {
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("com.wontlost:zxing-vaadin:2.0.2-8")

//    implementation("org.springframework.data:spring-data-jpa")
    implementation("org.springframework.data:spring-data-relational")

    implementation("dev.langchain4j:langchain4j-spring-boot-starter:0.34.0")
    implementation("dev.langchain4j:langchain4j-anthropic-spring-boot-starter:0.34.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("dev.voroby:spring-boot-starter-telegram:1.13.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}


dependencyManagement {
    imports {
        val vaadinVersion: String by project

        mavenBom("com.vaadin:vaadin-bom:$vaadinVersion")
    }
}
