plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("app.hohe.keyboard.tools.dictbuilder.MainKt")
}

dependencies {
    testImplementation(libs.junit)
}
