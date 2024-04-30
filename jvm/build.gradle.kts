import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
}

group = "pt.isel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktlint: Configuration by configurations.creating

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    ktlint("com.pinterest:ktlint:0.48.2") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // To access the *non-public* Continuation API
    // ONLY for learning purposes
    jvmArgs(listOf(
        "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED"
    ))
}
kotlin {
    jvmToolchain(21)
}

val outputDir = "${layout.buildDirectory}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))
val ktlintCheck by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("src/**/*.kt")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}

// Use the following to run:
// java -cp "build/libs/*:build/classes/kotlin/main" <package>.<class-name>Kt
tasks.register<Copy>("packLibs") {
    from(configurations.runtimeClasspath)
    into("build/libs")
}