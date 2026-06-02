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

uniform vec3 ambientColor;

uniform int lightCount;
uniform vec4 lightColor[MAX_LIGHTS];
uniform vec4 lightDir[MAX_LIGHTS];
uniform vec4 lightPos[MAX_LIGHTS];
uniform vec4 lightParams[MAX_LIGHTS];
uniform mat4 lightSpaceMatrices[MAX_LIGHTS];
uniform sampler2D shadowMaps[MAX_LIGHTS];

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

        float shadow = 1.0;
        bool hasShadow = lightParams[i].z > 0.5;
        if (hasShadow) {
            vec4 lsPos = lightSpaceMatrices[i] * vec4(fragWorldPos, 1.0);
            vec3 proj = lsPos.xyz / lsPos.w;
            vec2 shadowUV = proj.xy * 0.5 + 0.5;
            if (shadowUV.x >= 0.0 && shadowUV.x <= 1.0 && shadowUV.y >= 0.0 && shadowUV.y <= 1.0) {
                float closest = texture(shadowMaps[i], shadowUV).r;
                float bias = 0.002;
                shadow = proj.z >= closest - bias ? 1.0 : 0.0;
            }
        }

        colorSum += lColor * diff * attenuation * shadow;
    }

    colorSum += ambientColor;
    fragColor = texture(textureSampler, a_uv) * vec4(colorSum, 1.0);
}
