plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlin.reflect)

    testImplementation(kotlin("test"))
    testImplementation(libs.sqlite.jdbc)
}

application {
    mainClass.set("io.kaiteyo.kjd.cli.KjdCliKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
