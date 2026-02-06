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

public interface ClimateEvent {

	long getStartTime();

	long getCalmEndTime();

	ClimateResult getHourClimate(long t);

	/**
	 * Compute the temperature at a given time according to this temperature event.
	 * This algorithm is based on a piecewise interpolation technique.
	 *
	 * @param t given in seconds.
	 * @return temperature at given time.
	 * @author JackyWangMislantiaJnirvana <wmjwld@live.cn>
	 */
	float getHourTemp(long t);

}