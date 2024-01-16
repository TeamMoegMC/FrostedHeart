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
import com.teammoeg.frostedheart.scenario.runner.target.ActTarget;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

public class SceneCommand {
	public void startAct(ScenarioConductor runner,@Param("c")String c,@Param("a")String a) {
		runner.endAct();
		runner.enterAct(new ActNamespace(c,a));
		//runner.jump(new ActTarget(new ActNamespace(c,a),runner.getCurrentAct().getCurrentPosition().next()));
	}
	public void endAct(ScenarioConductor runner) {
		runner.endAct();
	}
	public void queueAct(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("c")String c,@Param("a")String a) {
		
		runner.queueAct(new ActNamespace(c,a),s,l);
	}
	public void actTitle(ScenarioConductor runner,@Param("t")String t,@Param("st")String st) {
		if(t!=null)
			runner.getCurrentAct().title=t;
		if(st!=null)
			runner.getCurrentAct().subtitle=st;
	}
	public void startSystem(ScenarioConductor runner) {
		runner.enableActs();
	}
}
