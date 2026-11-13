#version 330 core
uniform vec4 tint;
uniform sampler2D image2d;
uniform sampler2DArray imageArray;
uniform int imageLayer;
uniform int imageKind;
in vec2 uv;
out vec4 fragColor;

void main() {
    fragColor = tint;
    if (imageKind == 1) fragColor *= texture(image2d, uv);
    if (imageKind == 2) fragColor *= texture(imageArray, vec3(uv, imageLayer));
}
