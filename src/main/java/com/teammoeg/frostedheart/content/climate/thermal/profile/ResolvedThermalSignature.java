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
        int materialStateId, int fullContactFaces, boolean airMatter, int materialTypeId, int contactPatches) {
    public ResolvedThermalSignature(int ventilation, int materialProfileId, int stateId, int faces,
            boolean airMatter, int typeId) {
        this(ventilation, materialProfileId, stateId, faces, airMatter, typeId, patchesForFullFaces(faces));
    }

    /** Four aligned quadrants per face, packed in X, Y, Z opposing face order. */
    public static int patchesForFullFaces(int faces) {
        int patches = 0;
        for (int face = 0; face < 6; face++) if ((faces & (1 << face)) != 0) patches |= 15 << (4 * face);
        return patches;
    }

    /** Exact boundary of a union of half-block octants (slabs and straight/corner stairs). */
    public static int patchesForOctants(int octants) {
        int patches = 0;
        for (int octant = 0; octant < 8; octant++) {
            if ((octants & 1 << octant) == 0) continue;
            int x = octant & 1, y = (octant >> 1) & 1, z = octant >> 2;
            patches |= 1 << (4 * x + 2 * z + y);
            patches |= 1 << (4 * (2 + y) + 2 * z + x);
            patches |= 1 << (4 * (4 + z) + 2 * y + x);
        }
        return patches;
    }

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
