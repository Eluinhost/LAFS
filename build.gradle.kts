
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.internal.SystemProperties
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.script.lang.kotlin.*
import org.jetbrains.kotlin.util.collectionUtils.concat
import proguard.gradle.ProGuardTask

group = "gg.uhc"
version = "1.0.0"

defaultTasks("clean", "proguard")

fun KotlinRepositoryHandler.maven(vararg mappings: Pair<String, String>) = mappings.forEach { this.maven { name = it.first; setUrl(it.second) }}
fun ShadowJar.relocateToNewBase(base: String, vararg packages: String) = packages.forEach { this.relocate(it, "$base.$it") }

task<Wrapper>("wrapper") {
    distributionUrl = "https://services.gradle.org/distributions-snapshots/gradle-3.1-20160819000024+0000-bin.zip"
}

buildscript {
    val shadowVersion = "1.2.+"
    val proguardVersion = "5.2.+"
    val kotlinGradleVersion = "1.1.0-dev-2053"

    repositories {
        gradleScriptKotlin()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlinGradleVersion))
        classpath("com.github.jengelman.gradle.plugins:shadow:$shadowVersion")
        classpath("net.sf.proguard:proguard-gradle:$proguardVersion")
    }
}

apply {
    arrayOf("kotlin", "idea", "com.github.johnrengelman.shadow").forEach { plugin(it) }
    plugin<ApplicationPlugin>()
}

repositories {
    mavenCentral()
    maven(
        "spigot" to "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
        "sonatype" to "https://oss.sonatype.org/content/repositories/snapshots",
        "jetbrains" to "http://repository.jetbrains.com/all"
    )
}

val kotlinVersion = "1.0.+"
val spigotVersion = "1.9-R0.1-SNAPSHOT"
val rxkotlinVersion = "0.+"
val spekVersion = "1.0.+"
val mockitoVersion = "1.10.+"

val basePackage = "${project.group}.${project.name.toLowerCase()}"
val shadePath = "$basePackage.shadow"
val mainClass = "$basePackage.Entry"

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    compile("org.spigotmc:spigot-api:$spigotVersion")
    compile("io.reactivex:rxkotlin:$rxkotlinVersion")

    testCompile("org.jetbrains.spek:spek:$spekVersion")
    testCompile("org.mockito:mockito-core:$mockitoVersion")
}

configure<ApplicationPluginConvention> {
    mainClassName = mainClass
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").compileClasspath += configurations.runtime
}

tasks.withType<JavaCompile> {
    options.isDebug = false
}

tasks.withType<ProcessResources> {
    expand(mapOf("name" to project.name, "version" to project.version, "mainClass" to mainClass))
}

tasks.withType<ShadowJar> {
    relocateToNewBase(shadePath, "kotlin", "org.jetbrains", "rx")

    dependencies { dependencyFilter.apply {
        arrayOf("org.jetbrains.kotlin:.*", "io.reactivex:rxkotlin:.*", "io.reactivex:rxjava:.*")
            .forEach { include(dependency(it)) }
    }}
}

tasks.create<ProGuardTask>("proguard") {
    dependsOn("shadowJar")

    injars("build/libs/${project.name}-${project.version}-all.jar")
    outjars("build/libs/${project.name}-${project.version}-min.jar")

    dontoptimize()
    dontobfuscate()

    libraryjars("${SystemProperties.getInstance().javaHomeDir}/lib/rt.jar")

    keepclasseswithmembers("""
        public class ${project.group}.${project.name.toLowerCase()}.Entry {
            public void onEnable();
        }
    """)
    keepattributes("Singature")

    configurations.compile.files.forEach { libraryjars(it) }
}
