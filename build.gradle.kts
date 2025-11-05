plugins {
    id("java-library")
}

group = "com.github.blackjack200.ouranos"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://jitpack.io")
}

dependencies {
    api(libs.org.allaymc.stateupdater.common)
    api(libs.org.allaymc.stateupdater.block.updater)
    api(libs.org.cloudburstmc.protocol.bedrock.connection)
    api(libs.cn.hutool.hutool.core)
    api(libs.org.projectlombok.lombok)
    api(libs.com.google.code.gson.gson)

    annotationProcessor(libs.org.projectlombok.lombok)
}