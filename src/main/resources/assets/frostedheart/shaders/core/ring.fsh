#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;

uniform float innerRadius; // 内半径
uniform float outerRadius; // 外半径
uniform float startAngle;  // 扇形起始角度（度）
uniform float endAngle;    // 扇形结束角度（度）

out vec4 fragColor;

float atan2(float y, float x) {
    const float PI2 = 6.2831853071795864769252867665590;
    return mod(atan(y,x) + (PI2*0.5), PI2);
}

void main() {
    vec2 center = vec2(0.5f,0.5f);
    float distance = distance(center, texCoord0);
    float angle = atan2(texCoord0.y - center.y,texCoord0.x - center.x);
    angle = degrees(angle);

    if (distance >= innerRadius && distance <= outerRadius && angle >= startAngle && angle <= endAngle) {
        fragColor = vertexColor;
    } else {
        discard;
    }
}
