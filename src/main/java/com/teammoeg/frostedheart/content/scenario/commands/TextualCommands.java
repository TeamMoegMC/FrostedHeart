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

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.network.S2CWaitTransMessage;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

import net.minecraft.server.level.ServerPlayer;

public class TextualCommands {
	public void nowait(ScenarioCommandContext runner) {
		runner.thread().scene().sendCached(runner.context(),runner.thread());
		runner.thread().scene().isNowait=true;
	}
	public void endnowait(ScenarioCommandContext runner) {
		runner.thread().scene().sendCached(runner.context(),runner.thread());
		runner.thread().scene().isNowait=false;
	}
	public void r(ScenarioCommandContext runner) {
		runner.thread().scene().sendNewLine(runner.context(),runner.thread(), RunStatus.RUNNING, false);
	}
	
	
	
	public void link(ScenarioCommandContext runner,@Param("lid")String linkId,@Param("s")String scenario,@Param("l")String label) {
		runner.thread().scene().sendCached(runner.context(),runner.thread());
		linkId=runner.thread().scene().createLink(linkId, scenario, label);
		Map<String,String> pars=new HashMap<>();
		pars.put("lid", linkId);
		FHScenario.callClientCommand("link", runner, pars);
	}
	public void endlink(ScenarioCommandContext runner) {
		runner.thread().scene().sendCached(runner.context(),runner.thread());
		Map<String,String> pars=new HashMap<>();
		FHScenario.callClientCommand("endlink", runner, pars);
	}
	public void nolink(ScenarioCommandContext runner) {
		runner.thread().scene().clearLink();
	}
	public void delay(ScenarioCommandContext runner,@Param("t")int t) {
		runner.thread().addWait(t);
		runner.thread().scene().sendCurrent(runner.context(),runner.thread(), RunStatus.WAITTIMER,false);
	}

}
