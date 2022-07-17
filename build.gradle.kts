import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import ProjectVersions.unethicaliteVersion

buildscript {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    java
    checkstyle
    kotlin("jvm") version "1.7.0"
}

project.extra["GithubUrl"] = "https://github.com/Blogans/hori-plugins"

apply<BootstrapPlugin>()

allprojects {
    group = "com.openosrs.externals"
    apply<MavenPublishPlugin>()
}

allprojects {
    apply<MavenPublishPlugin>()

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
}

subprojects {
    group = "com.openosrs.externals"

    project.extra["PluginProvider"] = "Hori"
    project.extra["ProjectSupportUrl"] = ""
    project.extra["PluginLicense"] = "3-Clause BSD License"

    repositories {
        jcenter {
            content {
                //excludeGroupByRegex("com\\.openosrs.*")
                excludeGroupByRegex("net\\.unethicalite.*")
            }
        }

        exclusiveContent {
            forRepository {
                mavenLocal()
            }
            filter {
                //includeGroupByRegex("com\\.openosrs.*")
                includeGroupByRegex("net\\.unethicalite.*")
            }
        }
    }

    apply<JavaPlugin>()

    dependencies {
        annotationProcessor(Libraries.lombok)
        annotationProcessor(Libraries.pf4j)

        //compileOnly("com.openosrs:http-api:$openosrsVersion+")
        //compileOnly("com.openosrs:runelite-api:$openosrsVersion+")
        //compileOnly("com.openosrs:runelite-client:$openosrsVersion+")
        //compileOnly("com.openosrs.rs:runescape-api:$openosrsVersion+")
        compileOnly("net.unethicalite:http-api:${ProjectVersions.unethicaliteVersion}+")
        compileOnly("net.unethicalite:runelite-api:${ProjectVersions.unethicaliteVersion}+")
        compileOnly("net.unethicalite:runelite-client:${ProjectVersions.unethicaliteVersion}+")
        compileOnly("net.unethicalite.rs:runescape-api:${ProjectVersions.unethicaliteVersion}+")
        compileOnly(Libraries.guice)
        compileOnly(Libraries.lombok)
        compileOnly(Libraries.pf4j)


    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                url = uri("$buildDir/repo")
            }
        }
        publications {
            register("mavenJava", MavenPublication::class) {
                from(components["java"])
            }
        }
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        withType<Jar> {
            doLast {
                copy {
                    from("./build/libs/")
                    into("../release/")
                }
            }
        }

        withType<AbstractArchiveTask> {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            dirMode = 493
            fileMode = 420
        }

        withType<Checkstyle> {
            group = "verification"

            exclude("**/ScriptVarType.java")
            exclude("**/LayoutSolver.java")
            exclude("**/RoomType.java")
        }

        register<Copy>("copyDeps") {
            into("./build/deps/")
            from(configurations["runtimeClasspath"])
        }
    }
}
repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}