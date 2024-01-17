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

package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

public class ControlCommands {

	public void jump(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.jump(new ExecuteTarget(scenario,label));
	}
	public void call(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.call(scenario, label);
	}
	public void queue(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.queue(new ExecuteTarget(scenario,label));
	}
	public void Return(ScenarioConductor runner) {
		runner.popCallStack();
	}
	public void macro(ScenarioConductor runner,@Param("name")String name) {
		runner.addMacro(name);

	}
	public void endmacro(ScenarioConductor runner) {
		runner.getExecutionData().remove("mp");
		runner.popCallStack();
	}

}
