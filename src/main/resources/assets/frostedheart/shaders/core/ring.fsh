/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */
#version 150

in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform vec4 innerColor;
uniform vec4 outerColor;

uniform float innerRadius; // 内半径
uniform float outerRadius; // 外半径
uniform float startAngle;  // 扇形起始角度（度）
uniform float endAngle;    // 扇形结束角度（度）

uniform float Smooth;    // 平滑度（抗锯齿）

out vec4 fragColor;

float atan2(float y, float x) {
    const float PI2 = 6.2831853071795864769252867665590;
    return mod(atan(y,x) + (PI2*0.5), PI2);
}

void main() {
    vec2 center = vec2(0.5f,0.5f);
    float distan = distance(center, texCoord0);
    vec2 translatedUV = texCoord0 - center;
    float radian = radians(startAngle+90.0F);
    mat2 rotationMatrix = mat2(cos(radian), -sin(radian), sin(radian), cos(radian));
    vec2 rotatedUV = rotationMatrix * translatedUV;
    vec2 finalUV = rotatedUV + center;
    float angle = atan2(finalUV.y - center.y,finalUV.x - center.x);
    angle = degrees(angle);

    float inAngleRange = smoothstep(0, 0 + Smooth, angle) * (1.0 - smoothstep(endAngle-startAngle - Smooth, endAngle-startAngle, angle));
    float inRadiusRange = smoothstep(innerRadius, innerRadius + Smooth, distan) * (1.0 - smoothstep(outerRadius - Smooth, outerRadius, distan));
    float inSector = inAngleRange * inRadiusRange;
    if (inSector == 0.0) {
        discard;
    }

    vec4 fColor = mix(innerColor, outerColor, (distan - innerRadius) / (outerRadius - innerRadius)) * ColorModulator;

    fragColor = fColor * ColorModulator;
    fragColor.a *= inSector;

}
