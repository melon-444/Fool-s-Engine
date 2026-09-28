#version 430

in vec2 vUv;

uniform vec3 uColor;
uniform float uTime;
uniform float uIntensity;

out vec4 fragColor;

void main() {
    vec2 p = vUv * 2.0 - 1.0;
    float r = length(p);
    float a = atan(p.y, p.x);

    // Two spiral arms that spin over time and twist with radius.
    float arms = 2.0;
    float twist = 6.0;
    float spiral = sin(arms * a + uTime * 4.0 + twist * r);
    float armsMask = pow(max(spiral, 0.0), 2.0);

    // Concentric rings that streak outward -> reads as orbiting particles.
    float rings = sin(r * 45.0 - uTime * 7.0);
    float ringMask = pow(0.5 + 0.5 * rings, 6.0);

    float glow = armsMask * (0.25 + 0.75 * ringMask);
    glow *= 1.0 - smoothstep(0.0, 1.0, r);
    glow *= uIntensity;

    fragColor = vec4(uColor * glow, glow);
}
