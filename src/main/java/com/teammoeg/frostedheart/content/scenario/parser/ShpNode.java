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

import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public class ShpNode implements Node {

    public ShpNode(String command, Map<String, String> params) {
        super();
    }

    @Override
    public String getLiteral(ScenarioCommandContext runner) {
        return "#";
    }

    @Override
    public String getText() {
        return "@sharp";
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public void run(ScenarioCommandContext runner) {

    }

}
