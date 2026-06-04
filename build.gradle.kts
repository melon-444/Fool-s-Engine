plugins {
    id("java-library")
}

group = "com.melon.foolsEngine"
version = "0.0.2"

val lwjglNatives = project.properties["lwjglNatives"] as? String ?: "windows"
val lwjglUseMaven = project.properties["lwjglUseMaven"] == "true"
val lwjglVersion = "3.3.6"
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
        api("io.github.spair:imgui-java-binding:$imguiVersion")
        api("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
        runtimeOnly("io.github.spair:imgui-java-natives-$lwjglNatives:$imguiVersion")
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
        api("io.github.spair:imgui-java-binding:$imguiVersion")
        api("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
        runtimeOnly("io.github.spair:imgui-java-natives-$lwjglNatives:$imguiVersion")
    }
}

val nmtJvmArgs = listOf("-XX:NativeMemoryTracking=detail")

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

tasks.register("runAllTests") {
    dependsOn("runTestBackend", "runTestInputBackend", "runTestLightBackend")
    group = "test with nmt"
    description = "Runs all test classes with NativeMemoryTracking=detail"
}
