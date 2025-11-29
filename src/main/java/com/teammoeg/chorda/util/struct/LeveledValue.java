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

package com.teammoeg.chorda.util.struct;

import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.CodecWithFactory;

public class LeveledValue {
	public static Codec<LeveledValue> createCodec(IntFunction<Float> maxValue) {
		
		return new CodecWithFactory<>(RecordCodecBuilder.create(t->t.group(
			Codec.INT.optionalFieldOf("level",0).forGetter(o->o.level),
			Codec.FLOAT.optionalFieldOf("value",0f).forGetter(o->o.value)
			).apply(t,(i,v)->new LeveledValue(maxValue,i,v))),()->new LeveledValue(maxValue));
		
	}
	
	IntFunction<Float> getMax;
	int level;
	float value;
	public void setValue(int level,int value) {
		this.level=level;
		this.value=value;
	}
	public LeveledValue(IntFunction<Float> getMax, int level, float value) {
		super();
		this.getMax = getMax;
		this.level = level;
		this.value = value;
	}
	public LeveledValue(IntFunction<Float> getMax) {
		super();
		this.getMax = getMax;
	}
    public void addValue(float value) {
    	this.value+=value;
    	float curmax=getCurrentMax();
    	while(this.value>=curmax) {
    		this.value-=curmax;
    		level++;
    		curmax=getCurrentMax();
    	}
    }
    public float getCurrentMax() {
    	return getMax.apply(level);
    }
    public void minValue(float value) {
    	this.value-=value;
    	while(this.value<=0) {
    		level--;
    		this.value+=getCurrentMax();
    	}
    	
    }
    public void minPercent(float value) {
    	this.value-=value*getCurrentMax();
    	while(this.value<=0) {
    		level--;
    		this.value+=getCurrentMax();
    	}
    }
	public int getLevel() {
		return level;
	}
	public float getValue() {
		return value;
	}
	@Override
	public String toString() {
		return "LeveledValue[level=" + level + ",value=" + value + "]";
	}
}
