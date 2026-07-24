plugins {
    id("java-library")
}

group = "com.melon.foolsEngine"
version = "0.1.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
        withSourcesJar()
}

val lwjglNatives = project.properties["lwjglNatives"] as? String ?: "windows"
val lwjglUseMaven = project.properties["lwjglUseMaven"] == "true"
val lwjglVersion = "3.4.1"
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
        runtimeOnly("org.lwjgl", "lwjgl",          classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-glfw",     classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-opengl",   classifier = "natives-$lwjglNatives")
        runtimeOnly("org.lwjgl", "lwjgl-stb",      classifier = "natives-$lwjglNatives")
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

        runtimeOnly(files("${libs}lwjgl/lwjgl-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-glfw/lwjgl-glfw-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-opengl/lwjgl-opengl-natives-${lwjglNatives}.jar"))
        runtimeOnly(files("${libs}lwjgl-stb/lwjgl-stb-natives-${lwjglNatives}.jar"))
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
    }
}

val nmtJvmArgs = listOf("-XX:NativeMemoryTracking=detail","-Xlint:deprecation")

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
