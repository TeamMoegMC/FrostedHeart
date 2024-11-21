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

package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.Arrays;

import com.teammoeg.frostedheart.util.evaluator.IEnvironment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class ScenarioVariables implements IScenarioVaribles  {
    CompoundTag extraData=new CompoundTag();
    CompoundTag snapshot;

    public ScenarioVariables() {
        super();
    }
    @Override
	public CompoundTag save() {
    	if(snapshot==null)
    		return new CompoundTag();
    	return snapshot;
    }
    
    @Override
	public void load(CompoundTag data) {
    	extraData=data;
    }
    @Override
	public void restoreSnapshot() {
    	if(snapshot!=null) {
    		extraData=snapshot;
    	}
    }
    @Override
	public boolean containsPath(String path) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        for (int i = 0; i < paths.length - 1; i++) {
            if (!nbt.contains(paths[i], 10))
                return false;
            nbt = nbt.getCompound(paths[i]);
        }
        return nbt.contains(paths[paths.length - 1]);
    }

    @Override
	public Tag evalPath(String path) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        for (int i = 0; i < paths.length - 1; i++) {
            nbt = nbt.getCompound(paths[i]);
        }
        return nbt.get(paths[paths.length - 1]);
    }

    @Override
	public Double evalPathDouble(String path) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        if(path.length()>1)
	        for (int i = 0; i < paths.length - 1; i++) {
	            nbt = nbt.getCompound(paths[i]);
	        }
        return nbt.getDouble(paths[paths.length - 1]);
    }

    @Override
	public String evalPathString(String path) {
        return evalPath(path).getAsString();
    }

    @Override
	public CompoundTag getExecutionData() {
    	if(extraData==null) {
    		extraData=new CompoundTag();
    	}
        return extraData;
    }
    @Override
	public void setPath(String path, Tag val) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundTag cnbt = new CompoundTag();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.put(paths[paths.length - 1], val);
    }

    @Override
	public void setPathNumber(String path, Number val) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundTag cnbt = new CompoundTag();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.putDouble(paths[paths.length - 1], val.doubleValue());
    }
    
    @Override
	public void setPathString(String path, String val) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundTag cnbt = new CompoundTag();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.putString(paths[paths.length - 1], val);
    }
    @Override
	public void takeSnapshot() {
    	snapshot= extraData.copy();
    }

    @Override
    public double get(String key) {

        return evalPathDouble(key);
    }
    @Override
    public Double getOptional(String key) {
        if (!containsPath(key))
            return null;
        return get(key);
    }

	@Override
    public void set(String key, double v) {
    	setPathNumber(key, v);
    }
	@Override
	public void remove(String path) {
        String[] paths = path.split("\\.");
        CompoundTag nbt = getExecutionData();
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundTag cnbt = new CompoundTag();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.remove(paths[paths.length - 1]);
	}
	@Override
	public CompoundTag getExtraData() {
		return extraData;
	}
	public CompoundTag getSnapshot() {
		return snapshot;
	}
}
