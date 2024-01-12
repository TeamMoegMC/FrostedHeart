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

public class EmbNode implements Node {
    String exp;
    String pat;
    String format;

    public EmbNode(String command, Map<String, String> params) {
        super();
        exp = params.get("exp");
        pat = params.get("var");
        format = params.getOrDefault("format", "%s");
    }

    @Override
    public void run(ScenarioRunner runner) {

    }

    @Override
    public String getText() {
        return "@emb exp=\"" + exp.replaceAll("\"", "\\\"") + "\"";
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public String getDisplay(ScenarioRunner runner) {
        Object dat = "";
        if (exp != null)
            dat = runner.eval(exp);
        else if (pat != null)
            dat = runner.evalPathString(pat);
        return String.format(format, dat);
    }

}
