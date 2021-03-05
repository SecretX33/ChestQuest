import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.github.secretx33"
version = "1.0"

repositories {
    jcenter()
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "codemc-repo"
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }
    maven {
        name = "gradle-repo"
        url = uri("https://plugins.gradle.org/m2/")
    }
}

dependencies {
    val kotlinVersion: String by System.getProperties()
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT") // Spigot API dependency
    compileOnly(fileTree("libs"))      // Spigot server dependency
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("com.squareup.moshi:moshi:1.11.0")
    val koin_version = "2.2.2"
    implementation("org.koin:koin-core:$koin_version")
    testCompileOnly("org.koin:koin-test:$koin_version")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("com.zaxxer:HikariCP:4.0.2")
}

// Disables the normal jar task
tasks.jar { enabled = false }

// And enables shadowJar task
artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set(rootProject.name + ".jar")
//    mergeServiceFiles()
//    exclude("module-info.class")
}

tasks.register("customCleanUp", Delete::class){
    delete("$rootDir/build/libs/${tasks.shadowJar.get().archiveFileName.get()}")
}
tasks.shadowJar.get().dependsOn(tasks["customCleanUp"])

tasks.test { useJUnitPlatform() }

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

tasks.processResources {
    expand("name" to rootProject.name, "version" to project.version)
}
