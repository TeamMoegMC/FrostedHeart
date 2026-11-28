#version 150

in vec3 Position;
out vec2 clipXY;

void main() {
    clipXY = Position.xy;
    gl_Position = vec4(clipXY, 0.0, 1.0);
}
