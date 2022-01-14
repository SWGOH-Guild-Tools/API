import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.ByteArrayOutputStream

class ReleasePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.task("release") {
            doFirst {
                // Grab the version, and bump it
                val version = target.version.toString().split('.').toMutableList()
                version.removeLast()
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

                val runner = System.getenv("GITHUB_RUN_NUMBER")
                // Save the new version
                // <major>.<minor>.<patch>
                val suffix = if(isMain) "${minor}.${runner}" else "${minor}.${runner}-SNAPSHOT"
                target.version = "${version[0]}.${suffix}"

                // Write the changes to the file
                val file = File("${project.projectDir}/build.gradle.kts")
                val s = file.readText().split('\n').toMutableList()
                for (i in s.indices) {
                    if(s[i].startsWith("version")) {
                        s[i] = "version = \"${target.version}\""
                    }
                }
                file.writeText(s.joinToString("\n"))

                val release = if(isMain) "latest" else "snapshot"
                File("${project.projectDir}/release.txt").writeText(release)
                File("${project.projectDir}/tag.txt").writeText(target.version.toString())

                // On release, update application.properties to set mongo connection to 0.0.0.0 instead of external
                val applicationFile = File("./src/main/resources/application.properties")
                val data = applicationFile.readText().split('\n').toMutableList()
                for (i in data.indices) {
                    if(data[i].startsWith("spring.data.mongodb.host")) {
                        data[i] = "spring.data.mongodb.host=0.0.0.0:27017"
                    }
                }
                applicationFile.writeText(data.joinToString("\n"))
            }
        }
    }
}

apply<ReleasePlugin>()

plugins {
    id("org.springframework.boot") version "2.6.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    id("com.netflix.dgs.codegen") version "5.1.14"
}

group = "io.guildtools"
version = "0.14.84"
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
