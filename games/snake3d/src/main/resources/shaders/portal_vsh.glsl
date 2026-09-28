#version 430

layout(location = 0) in vec3 position;
layout(location = 3) in mat4 instanceModel;

uniform mat4 vp;

out vec3 vWorldPos;

void main() {
    vec4 wp = instanceModel * vec4(position, 1.0);
    vWorldPos = wp.xyz;
    gl_Position = vp * wp;
}
