package com.teammoeg.frostedheart.content.scenario.runner;

import com.teammoeg.chorda.math.evaluator.IEnvironment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public interface IScenarioVaribles extends IEnvironment{

	CompoundTag save();

	void load(CompoundTag data);

	void restoreSnapshot();

	boolean containsPath(String path);

	Tag evalPath(String path);

	Double evalPathDouble(String path);

	String evalPathString(String path);

	CompoundTag getExecutionData();

	void setPath(String path, Tag val);

	void setPathNumber(String path, Number val);

	void setPathString(String path, String val);

	void takeSnapshot();

	double get(String key);

	Double getOptional(String key);

	void set(String key, double v);

	CompoundTag getExtraData();

	void remove(String path);

}