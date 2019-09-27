import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val assertJVersion = "3.10.0"
val junitPlatformVersion = "1.2.0"
val junitJupiterVersion = "5.2.0"

version = "0.0.1-SNAPSHOT"
group = "net.lab0.codingame"

buildscript {
  repositories {
    mavenCentral()
    jcenter()
  }
  dependencies {
    classpath("org.junit.platform:junit-platform-gradle-plugin:+")
  }
}

plugins {
  val kotlinVersion = "1.3.0"
  idea
  java
  id("org.jetbrains.kotlin.jvm") version kotlinVersion
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

idea {
  module {
    isDownloadSources = true
  }
}

repositories {
  mavenLocal()
  mavenCentral()
  jcenter()
}

dependencies {

  compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  compile("org.jetbrains.kotlin:kotlin-reflect")

  testImplementation("org.funktionale:funktionale-all:1.2")

  testImplementation("org.assertj:assertj-core:$assertJVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
