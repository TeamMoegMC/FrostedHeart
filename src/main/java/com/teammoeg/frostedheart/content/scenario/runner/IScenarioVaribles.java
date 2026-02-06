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