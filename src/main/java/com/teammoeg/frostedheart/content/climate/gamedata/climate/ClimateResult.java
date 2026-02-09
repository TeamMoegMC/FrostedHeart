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

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

public record ClimateResult(float temperature,ClimateType climate) {
	public static final ClimateResult EMPTY=new ClimateResult(0f,ClimateType.NONE);
	public ClimateResult setClimate(ClimateType climate) {
		return new ClimateResult(temperature,climate);
	}
	
	public ClimateResult setTemperature(float temperature) {
		return new ClimateResult(temperature,climate);
	}
	public ClimateResult merge(ClimateResult other) {
		if(this.climate.merge(other.climate())==climate&&this.temperature<other.temperature()) {
			return this;
		}
		if(this.climate.merge(other.climate())==other.climate&&this.temperature>other.temperature()) {
			return other;
		}
		return new ClimateResult(Math.min(temperature, other.temperature()),climate.merge(other.climate()));
	}
}
