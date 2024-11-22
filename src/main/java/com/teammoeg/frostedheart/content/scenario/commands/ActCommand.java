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
import com.teammoeg.frostedheart.content.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.content.scenario.runner.ActScenarioContext;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public class ActCommand {
	public void act(ScenarioCommandContext vrunner,@Param("c")String c,@Param("a")String a) {
		if(vrunner.context() instanceof ActScenarioContext context) {
			context.conductor().enterAct(new ActNamespace(c,a));
		}

		//runner.jump(new ActTarget(new ActNamespace(c,a),runner.getCurrentAct().getCurrentPosition().next()));
	}
	public void endAct(ScenarioCommandContext vrunner) {
		if(vrunner.context() instanceof ActScenarioContext context) {
			context.conductor().endAct();
		}
	}
	public void startAct(ScenarioCommandContext vrunner,@Param("s")String s,@Param("l")String l,@Param("c")String c,@Param("a")String a) {
		if(vrunner.context() instanceof ActScenarioContext context) {
			context.conductor().queueAct(new ActNamespace(c,a),s,l);
		}
	}
	public void actTitle(ScenarioCommandContext vrunner,@Param("t")String t,@Param("st")String st) {
		if(vrunner.context() instanceof ActScenarioContext context) {
			context.conductor().getCurrentAct().setTitles(vrunner.context(),t, st);
		}
	}
	public void startSystem(ScenarioCommandContext vrunner) {
		if(vrunner.context() instanceof ActScenarioContext context) {
			context.conductor().enableActs();
		}
	}
}
