#version 330 core
uniform vec4 rect;
out vec2 uv;

void main() {
    vec2 corner = vec2(gl_VertexID & 1, (gl_VertexID >> 1) & 1);
    uv = vec2(corner.x, 1.0 - corner.y);
    gl_Position = vec4(rect.xy + corner * rect.zw, 0.0, 1.0);
}
