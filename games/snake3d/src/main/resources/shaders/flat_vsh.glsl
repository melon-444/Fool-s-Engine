#version 430

layout(location = 0) in vec3 position;
layout(location = 2) in vec3 normal;
layout(location = 3) in mat4 instanceModel;

uniform mat4 vp;

out vec3 vNormal;

void main() {
    gl_Position = vp * instanceModel * vec4(position, 1.0);
    vNormal = mat3(instanceModel) * normal;
}
