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

package com.teammoeg.chorda.util.struct;

import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.CodecWithFactory;

/**
 * 分级数值，表示一个带等级和经验值的数值系统。
 * 当数值超过当前等级上限时自动升级，支持增减操作。
 * <p>
 * Leveled value representing a numeric system with level and experience.
 * Automatically levels up when the value exceeds the current level cap.
 * Supports addition and subtraction operations.
 */
public class LeveledValue {
	/**
	 * 创建LeveledValue的编解码器。
	 * <p>
	 * Create a codec for LeveledValue.
	 *
	 * @param maxValue 根据等级返回当前等级上限的函数 / a function that returns the max value for the given level
	 * @return 编解码器 / the codec
	 */
	public static Codec<LeveledValue> createCodec(IntFunction<Float> maxValue) {
		
		return new CodecWithFactory<>(RecordCodecBuilder.create(t->t.group(
			Codec.INT.optionalFieldOf("level",0).forGetter(o->o.level),
			Codec.FLOAT.optionalFieldOf("value",0f).forGetter(o->o.value)
			).apply(t,(i,v)->new LeveledValue(maxValue,i,v))),()->new LeveledValue(maxValue));
		
	}
	
	IntFunction<Float> getMax;
	int level;
	float value;

	/**
	 * 直接设置等级和经验值。
	 * <p>
	 * Directly set the level and experience value.
	 *
	 * @param level 等级 / the level
	 * @param value 经验值 / the experience value
	 */
	public void setValue(int level,int value) {
		this.level=level;
		this.value=value;
	}

	/**
	 * 使用指定的上限函数、初始等级和初始值构造分级数值。
	 * <p>
	 * Construct a leveled value with the specified max function, initial level and initial value.
	 *
	 * @param getMax 根据等级返回上限的函数 / the function that returns the cap for a given level
	 * @param level 初始等级 / the initial level
	 * @param value 初始值 / the initial value
	 */
	public LeveledValue(IntFunction<Float> getMax, int level, float value) {
		super();
		this.getMax = getMax;
		this.level = level;
		this.value = value;
	}

	/**
	 * 使用指定的上限函数构造分级数值，等级和值初始为0。
	 * <p>
	 * Construct a leveled value with the specified max function, level and value default to 0.
	 *
	 * @param getMax 根据等级返回上限的函数 / the function that returns the cap for a given level
	 */
	public LeveledValue(IntFunction<Float> getMax) {
		super();
		this.getMax = getMax;
	}
    /**
     * 增加数值，超过当前等级上限时自动升级。
     * <p>
     * Add value, automatically leveling up when the current level cap is exceeded.
     *
     * @param value 要增加的值 / the value to add
     */
    public void addValue(float value) {
    	this.value+=value;
    	float curmax=getCurrentMax();
    	while(this.value>=curmax) {
    		this.value-=curmax;
    		level++;
    		curmax=getCurrentMax();
    	}
    }
    /**
     * 获取当前等级的上限值。
     * <p>
     * Get the maximum value for the current level.
     *
     * @return 当前等级上限 / the current level cap
     */
    public float getCurrentMax() {
    	return getMax.apply(level);
    }
    /**
     * 减少数值，不足时自动降级。
     * <p>
     * Subtract value, automatically leveling down when insufficient.
     *
     * @param value 要减少的值 / the value to subtract
     */
    public void minValue(float value) {
    	this.value-=value;
    	while(this.value<=0) {
    		level--;
    		this.value+=getCurrentMax();
    	}
    	
    }
    /**
     * 按百分比减少数值（相对于当前等级上限），不足时自动降级。
     * <p>
     * Subtract a percentage of the current level cap, automatically leveling down when insufficient.
     *
     * @param value 百分比值（0-1） / the percentage value (0-1)
     */
    public void minPercent(float value) {
    	this.value-=value*getCurrentMax();
    	while(this.value<=0) {
    		level--;
    		this.value+=getCurrentMax();
    	}
    }
	/** 获取当前等级。 / Get the current level. */
	public int getLevel() {
		return level;
	}
	/** 获取当前经验值。 / Get the current experience value. */
	public float getValue() {
		return value;
	}
	@Override
	public String toString() {
		return "LeveledValue[level=" + level + ",value=" + value + "]";
	}
}
