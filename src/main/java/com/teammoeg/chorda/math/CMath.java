/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class CMath {

    public static final Random RANDOM = new Random();

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    public static <T> T selectElementByTime(T[] list) {
    	return list[(int) ((System.currentTimeMillis() / 1000) %list.length)];
    }
	public static <T> T selectElementByTime(List<T> list) {
		return list.get((int) ((System.currentTimeMillis() / 1000) %list.size()));
	}
	public static int randomValue(RandomSource rs,double rate) {
		if (rate > 0) {
			int total = (int) rate;
			double npart = Mth.frac(rate);
			if (npart>0&&rs.nextDouble() < npart) {
				total++;
			}
			return total;
		}
		return 0;
	}
}

