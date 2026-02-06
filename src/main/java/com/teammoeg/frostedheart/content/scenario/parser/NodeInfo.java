/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.scenario.parser;

import com.teammoeg.chorda.util.parsereader.ParseReader.ParserState;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public record NodeInfo(Node node,ParserState state) implements Node{

	@Override
	public String getLiteral(ScenarioCommandContext scenarioVM) {
		try {
			return node.getLiteral(scenarioVM);
		}catch(Throwable t) {
			throw state.generateException(t);
		}
	}

	@Override
	public String getText() {
		return node.getText();
	}

	@Override
	public boolean isLiteral() {
		return node.isLiteral();
	}

	@Override
	public void run(ScenarioCommandContext scenarioVM) {
		try {
			node.run(scenarioVM);
		}catch(Throwable t) {
			throw state.generateException(t);
		}
	}
	
	
}