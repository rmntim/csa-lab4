plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "ru.rmntim"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass = "ru.rmntim.csa4.asm.MainKt"
}

dependencies {
    implementation(project(":isa"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Main-Class"] = "ru.rmntim.csa4.asm.MainKt"
    }
    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
