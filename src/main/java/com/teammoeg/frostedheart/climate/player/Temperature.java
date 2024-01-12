/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate.player;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

/**
 * The core of our dynamic body & environment temperature system
 *
 * @author yuesha-yc
 * @author khjxiaogu
 */
public class Temperature {

	public static final String DATA_ID = FHMain.MODID + ":data";

	/**
	 * Get the body temperature on the basis of 37 Celsius degree.
	 * Avoid using this method directly, use {@link #getBodySmoothed(PlayerEntity)} instead.
	 * <br>
	 * Example: return -1 when body temp is 36C.
	 * @param spe the player
	 * @return the body temperature
	 */
	public static float getBody(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			return 0;
		return nc.getFloat("bodytemperature");
	}

	/**
	 * Get the delta temperature since last update (10 ticks ago)
	 * <p>
	 *     delta = last - current
	 * </p>
	 *
	 * @param spe the player
	 * @return the delta temperature
	 */
	public static float getBodyDelta(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			return 0;
		return nc.getFloat("deltatemperature");
	}

	/**
	 * Get a smoothed temperature value based on current body temperature and
	 * delta temperature since last update (10 ticks ago).
	 * <p>
	 * You should use this instead of {@link #getBody(PlayerEntity)}
	 * when you want to display the temperature to the player.
	 * </p>
	 * <p>
	 * The smoothing algorithm is as follows:
	 * <p>
	 * 1. Progress in [0, 9] which is the number of ticks since last update
	 * 2. The smoothed temperature is calculated as:
	 *   current + delta * (1 - progress / 10)
	 *   where delta = last - current
	 * </p>
	 * </p>
	 * <p>
	 * Example:
	 *  last = 37, current = 36, delta = 1
	 *  progress = 1
	 *  smoothed = 36 + 1 * (1 - 1 / 10) = 36.9
	 *  progress = 5
	 *  smoothed = 36 + 1 * (1 - 5 / 10) = 36.5
	 *  progress = 9
	 *  smoothed = 36 + 1 * (1 - 9 / 10) = 36.1
	 * </p>
	 * @param spe the player
	 * @return the smoothed temperature
	 */
	public static float getBodySmoothed(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		return nc.getFloat("smoothed_body_temperature");
	}

	/**
	 * On the basis of 0 Celsius degree.
	 * Example: return -20 when env temp is -20C.
	 */
	public static float getEnv(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			return 0;
		return nc.getFloat("envtemperature");
	}

	public static CompoundNBT getFHData(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			return new CompoundNBT();
		return nc;
	}

	public static void setFHData(PlayerEntity spe, CompoundNBT nc) {
		spe.getPersistentData().put(DATA_ID, nc);
	}

	public static void setBody(PlayerEntity spe, float val) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			nc = new CompoundNBT();
		nc.putFloat("bodytemperature", val);
		spe.getPersistentData().put(DATA_ID, nc);
	}

	public static void setEnv(PlayerEntity spe, float val) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			nc = new CompoundNBT();
		nc.putFloat("envtemperature", val);
		spe.getPersistentData().put(DATA_ID, nc);
	}

	public static void setBodySmoothed(PlayerEntity spe, float val) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		nc.putFloat("smoothed_body_temperature", val);
		spe.getPersistentData().put(DATA_ID, nc);
	}

	public static void set(PlayerEntity spe, float body, float env) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			nc = new CompoundNBT();
		// update delta before body
		nc.putFloat("deltatemperature", nc.getFloat("bodytemperature") - body);
		nc.putFloat("bodytemperature", body);
		nc.putFloat("envtemperature", env);
		spe.getPersistentData().put(DATA_ID, nc);
	}
}
