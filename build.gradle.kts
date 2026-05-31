plugins {
    id("java-library")
}

group = "com.melon.foolsEngine"
version = "0.0.0-preview"

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
