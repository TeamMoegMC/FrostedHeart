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

package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class ExecuteTarget extends ScenarioTarget{

	private final String label;
	public ExecuteTarget(String name, String label) {
		super(name);

		this.label = label;
	}
	public ExecuteTarget(Scenario sc, String label) {
		super(sc);

		this.label = label;
	}
	@Override
	public void accept(ScenarioConductor runner) {

		if(label!=null) {
			Integer ps=runner.getScenario().labels.get(label);
			if(ps!=null) {
				runner.gotoNode(ps);
			}
		}
	}
	
}