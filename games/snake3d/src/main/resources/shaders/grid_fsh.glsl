#version 430

in vec3 vWorldPos;

uniform vec3 uColor;
uniform float uTime;
uniform vec3 uHeadPos;
uniform float uRadius;

out vec4 fragColor;

void main() {
    // Bright only within uRadius of the head; faint everywhere else.
    float d = distance(vWorldPos, uHeadPos);
    float near = 1.0 - smoothstep(0.0, uRadius, d);
    float glow = 0.012 + 0.988 * near * near;
    glow *= 0.8 + 0.2 * sin(uTime * 3.0);
    fragColor = vec4(uColor * glow, 1.0);
}
