plugins {
    id("java-library")
}

group = "com.melon.foolsEngine"
version = "0.0.1-preview"

repositories {
    flatDir {
        dirs("lwjgl/lwjgl", "lwjgl/lwjgl-glfw", "lwjgl/lwjgl-opengl", "lwjgl/lwjgl-stb")
    }
    maven("https://jitpack.io/")
    mavenCentral()
}

dependencies {
    api(files("${rootProject.properties["lwjgl_libs"]}lwjgl/lwjgl.jar"))
    api(files("${rootProject.properties["lwjgl_libs"]}lwjgl-glfw/lwjgl-glfw.jar"))
    api(files("${rootProject.properties["lwjgl_libs"]}lwjgl-opengl/lwjgl-opengl.jar"))
    api(files("${rootProject.properties["lwjgl_libs"]}lwjgl-stb/lwjgl-stb.jar"))

    runtimeOnly(files("${rootProject.properties["lwjgl_libs"]}lwjgl/lwjgl-natives-windows.jar"))
    runtimeOnly(files("${rootProject.properties["lwjgl_libs"]}lwjgl-glfw/lwjgl-glfw-natives-windows.jar"))
    runtimeOnly(files("${rootProject.properties["lwjgl_libs"]}lwjgl-opengl/lwjgl-opengl-natives-windows.jar"))
    runtimeOnly(files("${rootProject.properties["lwjgl_libs"]}lwjgl-stb/lwjgl-stb-natives-windows.jar"))
    api("org.joml:joml:1.10.5")
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
