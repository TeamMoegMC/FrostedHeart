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

package com.teammoeg.chorda.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Chorda数学工具类，提供常用的数学运算辅助方法。
 * <p>
 * Mathematical utility class for Chorda, providing commonly used math helper methods.
 */
public class CMath {

    /** 全局共享的随机数生成器 / Global shared random number generator */
    public static final Random RANDOM = new Random();

    /**
     * 将double值四舍五入到指定的小数位数。
     * <p>
     * Rounds a double value to the specified number of decimal places using half-up rounding.
     *
     * @param value 要四舍五入的值 / the value to round
     * @param places 小数位数，必须非负 / number of decimal places, must be non-negative
     * @return 四舍五入后的值 / the rounded value
     * @throws IllegalArgumentException 如果places为负数 / if places is negative
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    /**
     * 根据当前时间从数组中选择一个元素，每秒切换一次。
     * <p>
     * Selects an element from the array based on the current time, cycling every second.
     *
     * @param list 元素数组 / the array of elements
     * @param <T> 元素类型 / element type
     * @return 根据当前时间选取的元素 / the element selected based on current time
     */
    public static <T> T selectElementByTime(T[] list) {
    	return list[(int) ((System.currentTimeMillis() / 1000) %list.length)];
    }
	/**
	 * 根据当前时间从列表中选择一个元素，每秒切换一次。
	 * <p>
	 * Selects an element from the list based on the current time, cycling every second.
	 *
	 * @param list 元素列表 / the list of elements
	 * @param <T> 元素类型 / element type
	 * @return 根据当前时间选取的元素 / the element selected based on current time
	 */
	public static <T> T selectElementByTime(List<T> list) {
		return list.get((int) ((System.currentTimeMillis() / 1000) %list.size()));
	}
	/**
	 * 根据给定的速率生成一个随机整数值，小数部分以概率方式进位。
	 * <p>
	 * Generates a random integer value from a rate, where the fractional part is probabilistically rounded up.
	 *
	 * @param rs 随机源 / the random source
	 * @param rate 速率值（非负） / the rate value (non-negative)
	 * @return 随机整数结果 / the randomized integer result
	 */
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
	/**
	 * 根据给定的速率生成一个随机整数值，小数部分以概率方式进位。使用Math.random()作为随机源。
	 * <p>
	 * Generates a random integer value from a rate, where the fractional part is probabilistically rounded up.
	 * Uses Math.random() as the random source.
	 *
	 * @param rate 速率值（非负） / the rate value (non-negative)
	 * @return 随机整数结果 / the randomized integer result
	 */
	public static int randomValue(double rate) {
		if (rate > 0) {
			int total = (int) rate;
			double npart = Mth.frac(rate);
			if (npart>0&&Math.random() < npart) {
				total++;
			}
			return total;
		}
		return 0;
	}
	/**
	 * 检查一个浮点值是否在指定的闭区间内。
	 * <p>
	 * Checks whether a float value is within the specified closed range [min, max].
	 *
	 * @param value 要检查的值 / the value to check
	 * @param min 最小值（含） / the minimum value (inclusive)
	 * @param max 最大值（含） / the maximum value (inclusive)
	 * @return 如果值在范围内则返回true / true if the value is within the range
	 */
	public static boolean inRange(float value,float min,float max) {
    	return value>=min&&value<=max;
    }
	/**
	 * 将浮点值转换为有效的有限值。NaN转为0，无穷大转为极大有限值。
	 * <p>
	 * Converts a float to a valid finite value. NaN becomes 0, infinities become large finite values.
	 *
	 * @param origin 原始浮点值 / the original float value
	 * @return 有效的有限浮点值 / a valid finite float value
	 */
	public static float toValidValue(float origin) {
		if(Float.isFinite(origin))
			return origin;
		if(Float.isNaN(origin))
			return 0;
		if(origin==Float.NEGATIVE_INFINITY)
			return -8388605;
		if(origin==Float.POSITIVE_INFINITY)
			return 8388606;
		return 0;
	}
	/**
	 * 将浮点值转换为有效值并夹紧到指定范围内。NaN和负无穷返回min，正无穷返回max。
	 * <p>
	 * Converts a float to a valid value clamped to the specified range. NaN and negative infinity return min,
	 * positive infinity returns max.
	 *
	 * @param origin 原始浮点值 / the original float value
	 * @param min 最小值 / the minimum value
	 * @param max 最大值 / the maximum value
	 * @return 夹紧后的有效浮点值 / the valid clamped float value
	 */
	public static float toValidClampedValue(float origin,float min,float max) {
		if(Float.isFinite(origin))
			return Mth.clamp(origin, min, max);
		if(Float.isNaN(origin))
			return min;
		if(origin==Float.NEGATIVE_INFINITY)
			return min;
		if(origin==Float.POSITIVE_INFINITY)
			return max;
		return min;
	}
}

