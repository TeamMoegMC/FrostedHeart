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
package com.teammoeg.frostedheart.content.climate.thermal.profile;

/** Whole-block transport property and ordinary/phase material identity. */
public record ResolvedThermalSignature(int ventilation, int materialProfileId,
        int materialStateId, int fullContactFaces, boolean airMatter, int materialTypeId) {
    public ResolvedThermalSignature(int ventilation, int materialProfileId, int materialStateId,
            int fullContactFaces, boolean airMatter) {
        this(ventilation, materialProfileId, materialStateId, fullContactFaces, airMatter, -1);
    }
    public ResolvedThermalSignature(int ventilation, int materialProfileId) {
        this(ventilation, materialProfileId, -1, ventilation == 0 ? 63 : 0,
                ventilation == 100 && materialProfileId == 0);
    }
    public ResolvedThermalSignature {
        if (ventilation < 0 || ventilation > 100 || materialProfileId < 0)
            throw new IllegalArgumentException("invalid block thermal signature");
    }
}
