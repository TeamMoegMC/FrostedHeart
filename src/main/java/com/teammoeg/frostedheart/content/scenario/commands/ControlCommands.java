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
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;

public class ControlCommands {

	public void jump(ScenarioCommandContext runner,@Param("s")String scenario,@Param("l")String label) {
		runner.thread().jump(runner.context(),new ExecuteTarget(scenario,label));
	}

	public void queue(ScenarioCommandContext runner,@Param("s")String scenario,@Param("l")String label) {
		runner.thread().queue(new ExecuteTarget(scenario,label));
	}
	public void Return(ScenarioCommandContext runner) {
		runner.thread().popCallStack(runner.context());
	}
	public void macro(ScenarioCommandContext runner,@Param("name")String name) {
		runner.context().addMacro(name,runner.thread());

	}
	public void endmacro(ScenarioCommandContext runner) {
		runner.context().getVaribles().remove("mp");
		runner.thread().popCallStack(runner.context());
	}
	public void er(ScenarioCommandContext runner) {
		runner.thread().scene().clear(runner.context(),runner.thread(),RunStatus.RUNNING);
	}
	public void l(ScenarioCommandContext runner) {
		runner.thread().waitClient();
		runner.thread().scene().sendCurrent(runner.context(),runner.thread(), RunStatus.WAITCLIENT,false);
	}
	public void p(ScenarioCommandContext runner) {
    	if(runner.thread().scene().shouldWaitClient()&&!runner.thread().scene().isSlient()) {
    		runner.thread().setStatus(RunStatus.WAITCLIENT);
    		runner.thread().scene().markClearAfterClick();
    		runner.thread().scene().sendCurrent(runner.context(),runner.thread(),RunStatus.WAITCLIENT,false);
    	}else runner.thread().scene().clear(runner.context(),runner.thread(),RunStatus.RUNNING);
	}
	public void wc(ScenarioCommandContext runner) {
		runner.thread().waitClient();
		runner.thread().scene().sendCurrent(runner.context(),runner.thread(), RunStatus.WAITCLIENT,true);
	}
	public void wt(ScenarioCommandContext runner) {
		runner.thread().setStatus((RunStatus.WAITTRIGGER));
		runner.thread().scene().sendCurrent(runner.context(),runner.thread(), RunStatus.WAITTRIGGER,false);
	}
	public void wa(ScenarioCommandContext runner) {
		runner.thread().setStatus((RunStatus.WAITACTION));
		runner.thread().scene().sendCurrent(runner.context(),runner.thread(), RunStatus.WAITACTION,false);
	}
	public void s(ScenarioCommandContext runner) {
		runner.thread().stop();
		runner.thread().scene().sendCurrent(runner.context(),runner.thread(), RunStatus.STOPPED,false);
	}
	public void wr(ScenarioCommandContext runner) {
		runner.thread().setStatus((RunStatus.WAITRENDER));
		runner.context().getScene().waitRender(runner.thread(), false);
	}
	public void wtr(ScenarioCommandContext runner) {
		runner.thread().setStatus((RunStatus.WAITTRANS));
		runner.context().getScene().waitRender(runner.thread(), true);
	}
}
