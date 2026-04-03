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

import net.minecraft.util.Mth;

/**
 * 百分比转分数辅助工具，将百分比值四舍五入为自定义分母的整数分子，同时保持总份数不变。
 * <p>
 * A utility for rounding percentages into integer fractions of a custom denominator while preserving
 * the total number of parts.
 */
public class Persentage2FractionHelper {
	private float reminder=0;
	private int maximum;
	private int originMax;
	/**
	 * 构造一个指定分母的百分比转分数辅助器。
	 * <p>
	 * Constructs a percentage-to-fraction helper with the specified denominator.
	 *
	 * @param denum 分母，即总份数 / the denominator, i.e., total number of parts
	 */
	public Persentage2FractionHelper(int denum) {
		this.originMax=this.maximum=denum;
	}
	/**
	 * 将带小数的分数值四舍五入为整数，同时维护累计误差以保持总量守恒。
	 * <p>
	 * Rounds a fractional value to an integer while maintaining cumulative error to preserve total conservation.
	 *
	 * @param value 带小数的分数值 / the fractional value with decimals
	 * @return 四舍五入后的整数值 / the rounded integer value
	 */
	public int getRounded(float value) {
		value-=reminder;
		int retval=Math.round(value);
		reminder=retval-value;
		if(retval>maximum)
			retval=maximum;
		maximum-=retval;
		return retval;
	}
	/**
	 * 获取剩余未分配的份数。
	 * <p>
	 * Gets the remaining unallocated parts.
	 *
	 * @return 剩余份数 / the remaining parts
	 */
	public int getReminder() {
		return maximum;
	}
	/**
	 * 将百分比值转换为四舍五入的分数值。
	 * <p>
	 * Converts a percentage value to a rounded fraction value.
	 *
	 * @param value 百分比值，1.0表示100% / the percentage value, where 1.0 represents 100%
	 * @return 四舍五入后的整数分数值 / the rounded integer fraction value
	 */
	public int getPercentRounded(float value) {
		return getRounded(value*originMax);
	}
}
