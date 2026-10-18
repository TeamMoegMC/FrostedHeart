/*
 * Copyright (c) 2026 TeamMoeg
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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render.infrared;

/** Composition of our capture program; the backend's global shader loader stays untouched. */
public final class InfraredCaptureShader {
    private InfraredCaptureShader() {}

    public static String vertex(String original, String capture) {
        return original.replace("void main() {", capture + "\nvoid main() {")
                .replace("_vert_init();", "_vert_init();\n    fhReadTemperature();");
    }

    public static String fragment(String original) {
        return original.replace("out vec4 fragColor;", "out vec4 fragColor;\n"
                + "flat in int fhTemperature;\nlayout(location=1) out int fhSurfaceTemperature;")
                .replace("fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);",
                        "fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);\n"
                        + "    fhSurfaceTemperature = fhTemperature;");
    }
}
