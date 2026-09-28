#version 430

in vec3 vWorldPos;

uniform vec3 uColor;
uniform float uTime;

out vec4 fragColor;

void main() {
    // A glowing wave that travels along the portal frame.
    float wave = 0.5 + 0.5 * sin(uTime * 3.0 + (vWorldPos.x + vWorldPos.y + vWorldPos.z) * 0.8);
    float glow = 0.35 + 0.65 * wave;
    fragColor = vec4(uColor * glow, 1.0);
}
