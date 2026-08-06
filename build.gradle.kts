plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta10"
}

group = "com.chayewuu.hyperheartratebukkit"
version = "1.1.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    // Paper API 26.2（Minecraft 26.2）
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.7")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("Hyper-Heartrate-Bukkit-${project.version}.jar")
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}