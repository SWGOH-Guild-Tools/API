import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.util.Date

class VersionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.task("version") {
            val file = File("${project.projectDir}/build.gradle.kts")
            val s = file.readText().split('\n').toMutableList()
            for(i in s.indices) {
                if (s[i].startsWith("version")) {
                    val tag = s[i].split("=").last().trim().replace("\"", "")
                    target.exec {
                        commandLine = mutableListOf("echo", "TAG_NUMBER=${tag}", ">>", "\$GITHUB_ENV")
                    }
                }
            }
        }
    }
}

class ReleasePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.task("release") {
            doFirst {
                // Grab the version, and bump it
                val version = target.version.toString().split('.').toMutableList()
                var minor: Int = version.removeLast().toInt()

                // See what git branch we're on
                val output = ByteArrayOutputStream()
                target.exec {
                    commandLine = "git rev-parse --abbrev-ref HEAD".split(" ")
                    standardOutput = output
                }
                val branch = output.toString().trim()

                val isMain = branch == "main" || branch == "master"

                if(isMain) { minor += 1 }

                // Save the new version
                val suffix = if(isMain) minor.toString() else "${minor}-${Date().time / 1000}-SNAPSHOT"
                target.version = "${version.joinToString(".")}.${suffix}"

                // Write the changes to the file
                val file = File("${project.projectDir}/build.gradle.kts")
                val s = file.readText().split('\n').toMutableList()
                for (i in s.indices) {
                    if(s[i].startsWith("version")) {
                        s[i] = "version = \"${target.version}\""
                    }
                }
                file.writeText(s.joinToString("\n"))

                // If we're on the main or master branch, it's a release so commit the changes
                if(isMain) {

                    // Commit the change
                    target.exec {
                        commandLine = mutableListOf("git", "commit", "-a", "-m", "\"[ci skip] Bumping version number for release\"")
                    }
                    target.exec {
                        commandLine = mutableListOf("git", "tag", "v${target.version}")
                    }
                    // Push the change
                    target.exec {
                        commandLine = mutableListOf("git", "push", "--tags")
                    }
                }
            }
        }
    }
}

apply<ReleasePlugin>()
apply<VersionPlugin>()

plugins {
    id("org.springframework.boot") version "2.6.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    id("com.netflix.dgs.codegen") version "5.1.14"
}

group = "io.guildtools"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:latest.release"))
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter")
    implementation("help.swgoh.api:swgoh-api-connector:4.3.0")
}



tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.generateJava {
    schemaPaths =
        listOf("${projectDir}/src/main/resources/schema").toMutableList() // List of directories containing schema files
    packageName = "io.guildtools.swgraphql.model" // The package name to use to generate sources
    generateClient = true // Enable generating the type safe query API
}
