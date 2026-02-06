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

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.CommandNotFoundException;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public class CommandNode implements Node {
    String command;
    Map<String, String> params;
    String origText;
    public CommandNode(String command, Map<String, String> params,String origText) {
        super();
        this.command = command.toLowerCase();
        this.params = params;
        this.origText=origText;
    }

    @Override
    public String getLiteral(ScenarioCommandContext runner) {
        return "";
    }

    @Override
    public String getText() {
        return "@" + command + " " + params.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue().replaceAll("\"", "\"") + "\"").reduce("", (a, b) -> a + b + " ");
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public void run(ScenarioCommandContext runner) {
    	try {
    		runner.callCommand(command, params);
    	}catch(CommandNotFoundException ex) {
			runner.thread().appendLiteral(origText);
		} 
    }

}
