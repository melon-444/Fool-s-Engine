#version 430

in vec2 pass_textureCoords;
out vec4 fragColor;

uniform float time;
layout(binding = 0) uniform sampler2D textureSampler;

void main()
{
    fragColor = texture(textureSampler,pass_textureCoords);
    fragColor += vec4(sin(time)*.5, cos(time)*.5, -sin(time)*.5, 1.0);
}