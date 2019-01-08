import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.11"
    application
}

group = "com.codeparts.mongomonster"
version = "1.0"

repositories {
    mavenCentral()
    maven("http://maven.imagej.net/content/repositories/public/")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("no.tornado:tornadofx:1.7.17")
    compile("org.dockfx:DockFX:0.1.12")
    compile("org.litote.kmongo:kmongo:3.9.0")
    testCompile("junit", "junit", "4.12")
}

application {
    mainClassName = "$group.MongoMonsterApp"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.languageVersion = "1.3"
}

tasks.create<Jar>("distroJar") {
    setDuplicatesStrategy(DuplicatesStrategy.WARN)
    manifest {
        attributes(mapOf("Main-Class" to application.mainClassName))
    }
    val sourceMain = java.sourceSets["main"]
    from(sourceMain.output)

    configurations.runtimeClasspath.filter {
        it.name.endsWith(".jar")
    }.forEach { jar ->
        from(zipTree(jar))
    }
    exclude("'META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}
