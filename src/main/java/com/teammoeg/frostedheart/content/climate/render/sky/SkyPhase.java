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

package com.teammoeg.frostedheart.content.climate.render.sky;

/**
 * Phase functions of the sky, each normalized over the sphere.
 * The ice phase function is held by IceHaloProfile and IceHaloMap.
 */
public final class SkyPhase {
    private SkyPhase() {
    }

    public static double rayleigh(double cosine) {
        return 3.0 / (16.0 * Math.PI) * (1.0 + cosine * cosine);
    }

    public static double henyeyGreenstein(double cosine, double asymmetry) {
        double squared = asymmetry * asymmetry;
        double denominator = 1.0 + squared - 2.0 * asymmetry * cosine;
        return (1.0 - squared) / (4.0 * Math.PI * denominator * Math.sqrt(denominator));
    }
}
