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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public class IfNode implements Node {
    String cmd;
    String exp;
    Map<String,Integer> elsifs=new LinkedHashMap<>();
    int elseBlock = -1;

    public IfNode(String exp, int elseBlock) {
        super();
        this.exp = exp;
        this.elseBlock = elseBlock;
    }

    public IfNode(String cmd, Map<String, String> params) {
        this.cmd = cmd;
        exp = params.getOrDefault("exp","1");
    }

    @Override
    public String getLiteral(ScenarioCommandContext runner) {
        return "";
    }

    @Override
    public String getText() {
        return "@" + cmd + " exp=\"" + exp.replaceAll("\"", "\"") + "\"";
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public void run(ScenarioCommandContext runner) {
    	double val=runner.eval(exp);
    	//System.out.println(val+"/"+elseBlock);
        if (val <= 0) {
        	//System.out.println("else");
        	for(Entry<String, Integer> ent:elsifs.entrySet()) {
        		if(runner.eval(ent.getKey())>0) {
        			runner.thread().setExecutePos(ent.getValue());
        			return;
        		}
        	}
            runner.thread().setExecutePos(elseBlock);
        }
    }

}
