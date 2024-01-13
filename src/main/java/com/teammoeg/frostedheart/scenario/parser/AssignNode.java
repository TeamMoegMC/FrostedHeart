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

import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class AssignNode implements Node {
    String exp;
    String str;
    String pat;
    String pat2;

    public AssignNode(String command, Map<String, String> params) {
        super();
        exp = params.getOrDefault("exp", "0");
        pat = params.get("var");
        str = params.get("str");
        pat2 = params.get("var2");
    }

    @Override
    public String getDisplay(ScenarioConductor runner) {
        return "";
    }

    @Override
    public String getText() {
        return "@eval exp=\"" + exp.replaceAll("\"", "\\\"") + "\" str=\"" + str.replaceAll("\"", "\\\"") + "\"";
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public void run(ScenarioConductor runner) {
        if (pat2 != null) {
            runner.setPath(pat, runner.evalPath(pat2));
        } else if (str != null) {
            runner.setPathString(pat, str);
        } else {
            runner.setPathNumber(pat, runner.eval(exp));
        }
    }

}
