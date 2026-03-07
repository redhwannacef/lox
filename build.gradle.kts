plugins {
  application
  id("org.graalvm.buildtools.native") version "0.11.1"
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(libs.junit.jupiter)

  testRuntimeOnly(libs.junit.platform.launcher)
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

application {
  mainClass = "com.zor.Zor"
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
