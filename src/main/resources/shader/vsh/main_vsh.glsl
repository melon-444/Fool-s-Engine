#version 430


layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec3 normal;
layout(location = 3) in mat4 instanceModel;

out vec2 a_uv;
out float light_intensity;

uniform mat4 vp;

void main() {
    vec3 light_dir = normalize(vec3(0.5, 1.0, 0.5));
    light_intensity = max(dot(normalize(normal), light_dir), 0.0);

    gl_Position = vp * instanceModel * vec4(position, 1.0);
    a_uv = uv;
}