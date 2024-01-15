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

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

public class ControlCommands {
	public void delay(ScenarioConductor runner,@Param("t")int t) {
		//runner.waitClient();
		runner.getScene().addWait(t);
		runner.prepareTextualModification();

	}
	public void wt(ScenarioConductor runner) {
		runner.getCurrentAct().setStatus(RunStatus.WAITTRIGGER);
	}
	public void wa(ScenarioConductor runner) {
		runner.getCurrentAct().setStatus(RunStatus.WAITACTION);
	}
	public void s(ScenarioConductor runner) {
		runner.stop();
	}
	public void er(ScenarioConductor runner) {
		runner.getScene().clear();
	}
	public void l(ScenarioConductor runner) {
		runner.getScene().sendNoreline();
		runner.getScene().waitClient();
	}
	public void jump(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.getCurrentAct().jump(new ExecuteTarget(scenario,label));
	}
	public void call(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.call(scenario, label);
	}
	public void queue(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.getCurrentAct().queue(new ExecuteTarget(scenario,label));
	}
	public void Return(ScenarioConductor runner) {
		runner.popCallStack();
	}
	public void macro(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.call(scenario, label);

	}
	public void endmacro(ScenarioConductor runner) {
		runner.getExecutionData().remove("mp");
		runner.popCallStack();
	}
	public void link(ScenarioConductor runner,@Param("lid")String linkId,@Param("s")String scenario,@Param("l")String label) {
		runner.prepareTextualModification();
		linkId=runner.createLink(linkId, scenario, label);
		Map<String,String> pars=new HashMap<>();
		pars.put("lid", linkId);
		FHScenario.callClientCommand("link", runner, pars);
	}
	public void endlink(ScenarioConductor runner) {
		runner.prepareTextualModification();
		Map<String,String> pars=new HashMap<>();
		FHScenario.callClientCommand("endlink", runner, pars);
	}
	public void nolink(ScenarioConductor runner) {
		runner.clearLink();
	}
}
