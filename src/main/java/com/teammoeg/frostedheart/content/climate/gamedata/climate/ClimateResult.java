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
		ClimateType merged=this.climate.merge(other.climate());
		if(this==EMPTY)
			return other;
		if(other==EMPTY)
			return this;
		if(merged==climate&&this.temperature<=other.temperature()) {//avoid creating new object
			return this;
		}
		if(merged==other.climate&&this.temperature>=other.temperature()) {
			return other;
		}
		return new ClimateResult(Math.min(temperature, other.temperature()),merged);
	}
}
