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

package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.IScenarioConductor;

import net.minecraft.nbt.CompoundNBT;

public class ExecuteStackElement extends ScenarioTarget{
	private final int nodeNum;
	public ExecuteStackElement(IScenarioConductor par,String name, int nodeNum) {
		super(par,name);
		this.nodeNum = nodeNum;
	}
	public ExecuteStackElement(Scenario sc, int nodeNum) {
		super(sc);
		this.nodeNum = nodeNum;
	}
	public ExecuteStackElement(IScenarioConductor par,CompoundNBT n) {
		this(par,n.getString("storage"),n.getInt("node"));
	}

	public CompoundNBT save() {
		CompoundNBT nbt=new CompoundNBT();
		nbt.putString("storage", getName());
		nbt.putInt("node", nodeNum);
		return nbt;
	}
	public ExecuteStackElement next() {
		return new ExecuteStackElement(this.getScenario(),nodeNum+1);
	}
	@Override
	public void apply(IScenarioConductor conductor) {
		super.apply(conductor);
		conductor.setNodeNum(nodeNum);
	}

}