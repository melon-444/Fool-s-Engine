#version 430

#define MAX_LIGHTS 16
#define LIGHT_DIRECTIONAL 0
#define LIGHT_POINT 1
#define LIGHT_SPOT 2

in vec2 a_uv;
in vec3 fragWorldPos;
in vec3 fragWorldNormal;

out vec4 fragColor;

uniform sampler2D textureSampler;

uniform int lightCount;
uniform vec4 lightColor[MAX_LIGHTS];
uniform vec4 lightDir[MAX_LIGHTS];
uniform vec4 lightPos[MAX_LIGHTS];
uniform vec4 lightParams[MAX_LIGHTS];

void main() {
    vec3 N = normalize(fragWorldNormal);
    vec3 colorSum = vec3(0.0);

    for (int i = 0; i < lightCount && i < MAX_LIGHTS; i++) {
        int lightType = int(lightParams[i].x);
        vec3 lColor = lightColor[i].rgb;
        float intensity = lightColor[i].a;
        vec3 L;
        float attenuation = intensity;

        if (lightType == LIGHT_DIRECTIONAL) {
            L = normalize(-lightDir[i].xyz);
        } else if (lightType == LIGHT_POINT) {
            vec3 toLight = lightPos[i].xyz - fragWorldPos;
            float dist = length(toLight);
            L = toLight / dist;
            attenuation = intensity / (1.0 + 0.05 * dist + 0.005 * dist * dist);
        } else if (lightType == LIGHT_SPOT) {
            vec3 toLight = lightPos[i].xyz - fragWorldPos;
            float dist = length(toLight);
            L = toLight / dist;
            float theta = dot(-L, normalize(lightDir[i].xyz));
            float cutOff = lightParams[i].y;
            float spot = smoothstep(cutOff * 0.90, cutOff, theta);
            attenuation = intensity * spot / (1.0 + 0.05 * dist + 0.005 * dist * dist);
        }

        float diff = max(dot(N, L), 0.0);
        colorSum += lColor * diff * attenuation;
    }

    colorSum += vec3(0.06);
    fragColor = texture(textureSampler, a_uv) * vec4(colorSum, 1.0);
}
