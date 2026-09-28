#version 430

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;
layout(location = 3) in mat4 instanceModel;

uniform mat4 vp;

out vec2 vUv;

void main() {
    vUv = uv;
    gl_Position = vp * instanceModel * vec4(position, 1.0);
}
