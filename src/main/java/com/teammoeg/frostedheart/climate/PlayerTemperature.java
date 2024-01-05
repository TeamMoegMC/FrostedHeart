/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

/**
 * The core of our dynamic body & environment temperature system
 *
 * @author yuesha-yc
 * @author khjxiaogu
 */
public class PlayerTemperature {

	public static float getBlockTemp(ServerPlayerEntity spe) {
		/*long time = System.nanoTime();
		try {*/

		return new SurroundingTemperatureSimulator(spe).getBlockTemperature(spe.getPosX(), spe.getPosYEye(), spe.getPosZ());

		/*} finally {
			long delta = System.nanoTime() - time;
			System.out.println(String.format("total cost %.3f ms", (delta / 1000000f)));
		}*/

	}

	public static final String DATA_ID = FHMain.MODID + ":data";

	/**
	 * On the basis of 37 celsius degree.
	 * Example: return -1 when body temp is 36C.
	 */
	public static float getBodyTemperature(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			return 0;
		return nc.getFloat("bodytemperature");
	}

	public static float getLastTemperature(PlayerEntity spe) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			return 0;
		return nc.getFloat("lasttemperature");
	}

	/**
	 * On the basis of 0 celsius degree.
	 * Example: return -20 when env temp is -20C.
	 */
	public static float getEnvTemperature(PlayerEntity spe) {
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

	public static void setBodyTemperature(PlayerEntity spe, float val) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			nc = new CompoundNBT();
		nc.putFloat("bodytemperature", val);
		spe.getPersistentData().put(DATA_ID, nc);
	}

	public static void setEnvTemperature(PlayerEntity spe, float val) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			nc = new CompoundNBT();
		nc.putFloat("envtemperature", val);
		spe.getPersistentData().put(DATA_ID, nc);
	}

	public static void setTemperature(PlayerEntity spe, float body, float env) {
		CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
		if (nc == null)
			nc = new CompoundNBT();
		nc.putFloat("bodytemperature", body);
		nc.putFloat("envtemperature", env);
		nc.putFloat("deltatemperature", nc.getFloat("lasttemperature") - body);
		nc.putFloat("lasttemperature", body);
		spe.getPersistentData().put(DATA_ID, nc);
	}
}
