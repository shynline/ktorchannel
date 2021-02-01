plugins {
    `java-library`
    kotlin("jvm") version "1.4.20"
    maven
}

group = "com.github.shynline"
version = "1.0-beta1"
val ktorVersion = "1.5.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit", "junit", "4.12")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.lettuce:lettuce-core:6.0.2.RELEASE")

}
