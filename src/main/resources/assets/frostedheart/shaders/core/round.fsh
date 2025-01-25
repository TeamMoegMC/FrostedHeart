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