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

public class ParagraphNode implements Node {
    int nodeNum;

    public ParagraphNode(String command, Map<String, String> params) {
        super();
    }

    @Override
    public String getText() {
        return "@p";
    }

    @Override
    public String getDisplay(ScenarioRunner runner) {
        return "";
    }

    @Override
    public boolean isLiteral() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void run(ScenarioRunner runner) {
    }
}
