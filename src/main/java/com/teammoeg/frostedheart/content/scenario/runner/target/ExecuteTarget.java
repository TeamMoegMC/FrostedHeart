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

package com.teammoeg.frostedheart.content.scenario.runner.target;

import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

public record ExecuteTarget(String file,String label) implements ScenarioTarget{

	@Override
	public String toString() {
		return "ExecuteTarget [label=" + label + ", file=" + file + "]";
	}
	@Override
	public PreparedScenarioTarget prepare(ScenarioContext t, Scenario current) {
		Scenario scenario=file==null?current:t.loadScenario(file);

		return new PreparedScenarioTarget(scenario,scenario.labels.getOrDefault(label, 0));
	}

	
}