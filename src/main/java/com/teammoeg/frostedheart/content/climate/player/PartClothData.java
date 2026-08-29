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

package com.teammoeg.frostedheart.content.climate.player;

public class PartClothData {
	double thermalResistanceM2KPerW;
	double radiantHeatProof;
	double windProof;
	double waterResistance;

	public void set(
			double thermalResistanceM2KPerW,
			double radiantHeatProof,
			double windProof,
			double waterResistance
	) {
		this.thermalResistanceM2KPerW = Math.max(
				0.0D, thermalResistanceM2KPerW);
		this.radiantHeatProof = clamp01(radiantHeatProof);
		this.windProof = clamp01(windProof);
		this.waterResistance = clamp01(waterResistance);
	}

	private static double clamp01(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}

}
