#version 430

in vec2 a_uv;
in float light_intensity;

out vec4 fragColor;

uniform sampler2D textureSampler;

void main() {
    float ambient = 0.2;
    ambient = max(light_intensity, ambient);
    fragColor = texture(textureSampler,a_uv)*ambient;
}