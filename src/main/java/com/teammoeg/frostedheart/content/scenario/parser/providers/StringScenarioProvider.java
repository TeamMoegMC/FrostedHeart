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

package com.teammoeg.frostedheart.content.scenario.parser.providers;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;

public abstract class StringScenarioProvider implements ScenarioProvider {

	public StringScenarioProvider() {
	}
	public abstract String get(String t, Map<String, String> u);
	@Override
	public Scenario apply(String t, Map<String, String> u,String o) {
		String ss=get(t,u);
		if(ss==null)return null;
		return FHScenario.parser.parseString(o,ss);
	}

}
