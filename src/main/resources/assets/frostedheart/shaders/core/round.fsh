#version 150

in vec2 texCoord0;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5f,0.5f);
    float distance = distance(center, texCoord0);
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }
    if (distance <= 0.5) {
        fragColor = color * ColorModulator;
    } else {
        discard;
    }
}