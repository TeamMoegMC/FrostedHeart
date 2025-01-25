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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;

import net.minecraft.world.entity.player.Player;

public abstract class ScenarioContext {
	/** Variable data. */
	protected IScenarioVaribles varData=new DirectScenarioVariables();
	/** Macros. */
	Map<String,ExecuteStackElement> macros=new HashMap<>();
	public abstract Player player();
	public abstract void sendMessage(String string);
	public abstract String getLang();
	public Scenario loadScenario(String name) {
		return FHScenario.loadScenario(this, name);
	}
	public IScenarioVaribles getVarData() {
		return varData;
	}
	public IScenarioVaribles getVaribles() {
		return varData;
	}
	public Scenario loadScenarioIfNeeded(@Nullable String name,@Nullable Scenario current) {
		return (name==null||(name!=null&&current!=null&&name.equals(current.name())))?current:loadScenario(name);
	}
	
	/**
	 * Adds the macro.
	 *
	 * @param name the name
	 */
	public void addMacro(String name,ScenarioThread thread) {
		macros.put(name.toLowerCase(), thread.getCurrentPosition(1));
	}
	public void takeSnapshot() {
		varData.takeSnapshot();
	}
	public void restoreSnapshot() {
		varData.restoreSnapshot();
	}
}
