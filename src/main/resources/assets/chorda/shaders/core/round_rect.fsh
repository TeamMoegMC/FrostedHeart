#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float Radius;
uniform float Ratio;

out vec4 fragColor;

void main() {
    vec2 p = abs(step(0.5, texCoord0.xy) - texCoord0.xy);
    float edgeX = step(Radius, p.x);
    float edgeY = step(Radius, p.y * Ratio);
    float dist = step(length(vec2(p.x - Radius, p.y * Ratio - Radius)), Radius);
    float alpha = min(1.0, edgeX + edgeY + dist);
    vec4 color = vertexColor;
    color.a *= alpha;
    fragColor = color * ColorModulator;
}
