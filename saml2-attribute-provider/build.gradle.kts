import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("groovy")
    application
}

group = "io.curity.identityserver.plugin"
version = "1.0.4"
java.sourceCompatibility = JavaVersion.VERSION_17
description = "ICA SAML2 data access provider for use during JWT assertion"

repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus.curity.se/nexus/content/repositories/customer-release-repo")
    }
}

dependencies {
    compileOnly("se.curity.identityserver:identityserver.sdk:7.3.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.0")
    compileOnly("org.slf4j:slf4j-api:2.0.4")
    compileOnly("org.hibernate:hibernate-validator:7.0.4.Final")
    implementation("org.json:json:20230227")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("org.apache.santuario:xmlsec:2.3.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.3")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("com.onelogin:java-saml:2.9.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
        incremental = false
    }
}

application {
    mainClass.set("${rootProject.name}-$version")
}

val jar by tasks.getting(Jar::class) {
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
    archiveClassifier.set("") // Naming the jar
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
    from(contents)
}
tasks.register<Copy>("dist") {
    dependsOn(jar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(layout.buildDirectory.file("libs/${rootProject.name}-$version.jar"))
    var dest = "usr/share/plugins"
    if (System.getenv("IDSVR_HOME") != null) {
        dest = "${System.getenv("IDSVR_HOME")}/$dest"
    }
    into("$dest/${rootProject.name}")

    doLast {
        // Add version.txt file in project folder.
        file("$dest/${rootProject.name}/version.txt").writeText("$version")
    }
}
