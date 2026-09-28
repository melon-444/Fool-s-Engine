#version 430

in vec3 vNormal;

uniform vec3 uColor;

out vec4 fragColor;

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(vec3(0.5, 1.0, 0.4));
    float diff = max(dot(N, L), 0.0);
    float shade = 0.45 + 0.55 * diff;
    fragColor = vec4(uColor * shade, 1.0);
}
