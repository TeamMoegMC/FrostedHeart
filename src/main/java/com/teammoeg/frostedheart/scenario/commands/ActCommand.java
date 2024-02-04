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
import com.teammoeg.frostedheart.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class ActCommand {
	public void act(ScenarioVM vrunner,@Param("c")String c,@Param("a")String a) {
		ScenarioConductor runner;
		if(vrunner instanceof ScenarioConductor) {
			runner=(ScenarioConductor) vrunner;
		}else return;
		runner.endAct();
		runner.enterAct(new ActNamespace(c,a));
		//runner.jump(new ActTarget(new ActNamespace(c,a),runner.getCurrentAct().getCurrentPosition().next()));
	}
	public void endAct(ScenarioVM vrunner) {
		ScenarioConductor runner;
		if(vrunner instanceof ScenarioConductor) {
			runner=(ScenarioConductor) vrunner;
		}else return;
		runner.endAct();
	}
	public void startAct(ScenarioVM vrunner,@Param("s")String s,@Param("l")String l,@Param("c")String c,@Param("a")String a) {
		ScenarioConductor runner;
		if(vrunner instanceof ScenarioConductor) {
			runner=(ScenarioConductor) vrunner;
		}else return;
		runner.queueAct(new ActNamespace(c,a),s,l);
	}
	public void actTitle(ScenarioVM vrunner,@Param("t")String t,@Param("st")String st) {
		ScenarioConductor runner;
		if(vrunner instanceof ScenarioConductor) {
			runner=(ScenarioConductor) vrunner;
		}else return;
		runner.getCurrentAct().setTitles(t, st);
	}
	public void startSystem(ScenarioVM vrunner) {
		ScenarioConductor runner;
		if(vrunner instanceof ScenarioConductor) {
			runner=(ScenarioConductor) vrunner;
		}else return;
		runner.enableActs();
	}
}
