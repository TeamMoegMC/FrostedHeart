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

package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class IfNode implements Node {
    String cmd;
    String exp;
    int parentBlock = -1;
    int elseBlock = -1;
    int endBlock = -1;

    public IfNode(String cmd, Map<String, String> params) {
        this.cmd = cmd;
        exp = params.get("exp");
    }

    public IfNode(String exp, int parentBlock, int elseBlock, int endBlock) {
        super();
        this.exp = exp;
        this.parentBlock = parentBlock;
        this.elseBlock = elseBlock;
        this.endBlock = endBlock;
    }

    @Override
    public void run(ScenarioRunner runner) {
        if (runner.popCaller(parentBlock))
            return;

        if (runner.eval(exp) > 0) {
            runner.pushCall(parentBlock, endBlock);
        } else {
            runner.jump(elseBlock);
        }
    }

    @Override
    public String getText() {
        return "@" + cmd + " exp=\"" + exp.replaceAll("\"", "\\\"") + "\"";
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public String getDisplay(ScenarioRunner runner) {
        return "";
    }

}
