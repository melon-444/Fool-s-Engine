plugins {
    id("java-library")
    id ("com.diffplug.spotless") version("8.9.0")
}

group = "com.melon.foolsEngine"
version = "0.1.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
        withSourcesJar()
}

spotless {
    java {
        target ("src/**/*.java")
        licenseHeaderFile(
            rootProject.file("config/LICENSE_HEADER.txt"),
        )
    }
}

val lwjglNatives = project.properties["lwjglNatives"] as? String ?: "windows"
val lwjglUseMaven = project.properties["lwjglUseMaven"] == "true"
val lwjglVersion = "3.4.2"
val imguiVersion = "1.92.0"

if (lwjglUseMaven) {
    repositories {
        maven("https://jitpack.io/")
        mavenCentral()
    }

    dependencies {
        api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
        api("org.lwjgl", "lwjgl")
        api("org.lwjgl", "lwjgl-glfw")
        api("org.lwjgl", "lwjgl-opengl")
        api("org.lwjgl", "lwjgl-stb")
        api("org.lwjgl", "lwjgl-vulkan")
        api("org.lwjgl", "lwjgl-shaderc")
        runtimeOnly("org.lwjgl", "lwjgl",          classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-glfw",     classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-opengl",   classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-stb",      classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-shaderc", classifier = "natives-$lwjglNatives")
        api("org.joml:joml:1.10.5")
        implementation("org.ow2.asm:asm:9.10")
        implementation("org.ow2.asm:asm-commons:9.10")
        compileOnly("io.github.spair:imgui-java-binding:$imguiVersion")
        compileOnly("io.github.spair:imgui-java-lwjgl3:$imguiVersion"){
            exclude(group = "org.lwjgl")
        }
        runtimeOnly("io.github.spair:imgui-java-natives-$lwjglNatives:$imguiVersion")
        testImplementation("io.github.spair:imgui-java-binding:$imguiVersion")
        testImplementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion"){
            exclude(group = "org.lwjgl")
        }
        testRuntimeOnly("io.github.spair:imgui-java-natives-$lwjglNatives:$imguiVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }
} else {
    repositories {
        flatDir {
            dirs("lwjgl/lwjgl", "lwjgl/lwjgl-glfw", "lwjgl/lwjgl-opengl", "lwjgl/lwjgl-stb")
        }
        maven("https://jitpack.io/")
        mavenCentral()
    }

    val libs = rootProject.properties["lwjgl_libs"] as? String ?: "lwjgl/"

    dependencies {
        api(files("${libs}lwjgl/lwjgl.jar"))
        api(files("${libs}lwjgl-glfw/lwjgl-glfw.jar"))
        api(files("${libs}lwjgl-opengl/lwjgl-opengl.jar"))
        api(files("${libs}lwjgl-stb/lwjgl-stb.jar"))
        api(files("${libs}lwjgl-vulkan/lwjgl-vulkan.jar"))
        api(files("${libs}lwjgl-shaderc/lwjgl-shaderc.jar"))
        runtimeOnly(files("${libs}lwjgl/lwjgl-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-glfw/lwjgl-glfw-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-opengl/lwjgl-opengl-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-stb/lwjgl-stb-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-shaderc/lwjgl-shaderc-natives-${lwjglNatives}.jar"))
        implementation("org.ow2.asm:asm:9.10")
        implementation("org.ow2.asm:asm-commons:9.10")
        api("org.joml:joml:1.10.5")
        compileOnly("io.github.spair:imgui-java-binding:$imguiVersion")
        compileOnly("io.github.spair:imgui-java-lwjgl3:$imguiVersion"){
            exclude(group = "org.lwjgl")
        }
        runtimeOnly("io.github.spair:imgui-java-natives-$lwjglNatives:$imguiVersion")
        testImplementation("io.github.spair:imgui-java-binding:$imguiVersion")
        testImplementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion"){
            exclude(group = "org.lwjgl")
        }
        testRuntimeOnly("io.github.spair:imgui-java-natives-$lwjglNatives:$imguiVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }
}

val nmtJvmArgs = listOf("-XX:NativeMemoryTracking=detail")

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runTestBackend") {
    dependsOn(tasks.compileTestJava)
    group = "test with nmt"
    description = "Runs TestBackend with NativeMemoryTracking=detail"
    mainClass.set("com.melon.foolsEngineTest.TestBackend")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs(nmtJvmArgs)
}

tasks.register<JavaExec>("runTestInputBackend") {
    dependsOn(tasks.compileTestJava)
    group = "test with nmt"
    description = "Runs TestInputBackend with NativeMemoryTracking=detail"
    mainClass.set("com.melon.foolsEngineTest.TestInputBackend")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs(nmtJvmArgs)
}

tasks.register<JavaExec>("runTestLightBackend") {
    dependsOn(tasks.compileTestJava)
    group = "test with nmt"
    description = "Runs TestLightBackend with NativeMemoryTracking=detail"
    mainClass.set("com.melon.foolsEngineTest.TestLightBackend")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs(nmtJvmArgs)
}

tasks.register<JavaExec>("runTesECSRenderFlow") {
    dependsOn(tasks.compileTestJava)
    group = "test with nmt"
    description = "Runs TesECSRenderFlow (ECS + SystemScheduler) with NativeMemoryTracking=detail"
    mainClass.set("com.melon.foolsEngineTest.TesECSRenderFlow")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs(nmtJvmArgs)
}

tasks.register("runAllTests") {
    dependsOn("runTestBackend", "runTestInputBackend", "runTestLightBackend", "runTesECSRenderFlow")
    group = "test with nmt"
    description = "Runs all test classes with NativeMemoryTracking=detail"
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)

    from(sourceSets.main.get().output)
    from(sourceSets.test.get().output)

    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() && it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    val lwjglBase = file("lwjgl")
    if (lwjglBase.exists()) {
        for (subdir in lwjglBase.listFiles() ?: emptyArray()) {
            if (subdir.isDirectory) {
                from(fileTree(subdir) { include("*natives*.jar") }.map { zipTree(it) })
            }
        }
    }

    manifest {
        attributes["Main-Class"] = "com.melon.foolsEngineTest.TestLightBackend"
    }
}
