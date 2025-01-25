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
