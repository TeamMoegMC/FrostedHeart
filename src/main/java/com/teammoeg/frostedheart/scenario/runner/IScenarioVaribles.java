package com.teammoeg.frostedheart.scenario.runner;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public interface IScenarioVaribles {

	CompoundNBT save();

	void load(CompoundNBT data);

	void restoreSnapshot();

	boolean containsPath(String path);

	INBT evalPath(String path);

	Double evalPathDouble(String path);

	String evalPathString(String path);

	CompoundNBT getExecutionData();

	void setPath(String path, INBT val);

	void setPathNumber(String path, Number val);

	void setPathString(String path, String val);

	void takeSnapshot();

	double get(String key);

	Double getOptional(String key);

	void set(String key, double v);

}