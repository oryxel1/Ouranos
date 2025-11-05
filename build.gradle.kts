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
    api(libs.com.github.blackjack200.network)
    api(libs.cn.hutool.hutool.core)
    api(libs.cn.hutool.hutool.http)
    api(libs.org.apache.logging.log4j.log4j.core)
    api(libs.org.apache.logging.log4j.log4j.slf4j.impl)
    api(libs.org.apache.logging.log4j.log4j.api)
    api(libs.org.projectlombok.lombok)
    api(libs.com.google.code.gson.gson)
    api(libs.com.nimbusds.nimbus.jose.jwt)
    api(libs.net.minecrell.terminalconsoleappender)
}