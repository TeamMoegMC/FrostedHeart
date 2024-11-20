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

package com.teammoeg.frostedheart.content.scenario.commands;

import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.runner.BaseScenarioRunner;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;

public class ControlCommands {

	public void jump(BaseScenarioRunner runner,@Param("s")String scenario,@Param("l")String label) {
		runner.jump(new ExecuteTarget(runner,scenario,label));
	}
	public void call(BaseScenarioRunner runner,@Param("s")String scenario,@Param("l")String label) {
		runner.call(scenario, label);
	}
	public void queue(BaseScenarioRunner runner,@Param("s")String scenario,@Param("l")String label) {
		runner.queue(new ExecuteTarget(runner,scenario,label));
	}
	public void Return(BaseScenarioRunner runner) {
		runner.popCallStack();
	}
	public void macro(BaseScenarioRunner runner,@Param("name")String name) {
		runner.addMacro(name);

	}
	public void endmacro(BaseScenarioRunner runner) {
		runner.getVaribles().remove("mp");
		runner.popCallStack();
	}

}
