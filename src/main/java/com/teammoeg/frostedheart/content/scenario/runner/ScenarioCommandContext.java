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

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;

public record ScenarioCommandContext(ScenarioContext context,ScenarioThread thread) {

	public void callCommand(String command, Map<String, String> params) {
		thread.callCommand(this,command,params);
		
	}
	public double eval(String exp) {
		return thread.eval(context, exp);
	}
	public void jump(ScenarioTarget target) {
		thread.jump(context, target);
	}
}
