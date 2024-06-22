plugins {
    id("java")
}

group = "si.vegvamind"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
}