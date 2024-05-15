package com.teammoeg.frostedheart.util.utility;

import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.ConstructorCodec;
import com.teammoeg.frostedheart.util.io.CodecUtil;

public class LeveledValue {
	public static Codec<LeveledValue> createCodec(IntFunction<Float> maxValue) {
		
		return new ConstructorCodec<>(RecordCodecBuilder.create(t->t.group(
			CodecUtil.defaultValue(Codec.INT, 0).fieldOf("level").forGetter(o->o.level),
			CodecUtil.defaultValue(Codec.FLOAT, 0f).fieldOf("value").forGetter(o->o.value)
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
