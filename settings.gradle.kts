plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    id("com.gradle.develocity").version("3.17.1")
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.name = "csa4"

include("asm")
include("comp")
include("isa")
