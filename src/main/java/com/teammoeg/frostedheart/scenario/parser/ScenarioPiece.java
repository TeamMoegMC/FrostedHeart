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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScenarioPiece {
    public final String fileName;
    public final List<Node> pieces;
    public final int[] paragraphs;
    public final Map<String, Integer> labels;

    public ScenarioPiece(String fileName, List<Node> pieces, int[] paragraphs, Map<String, Integer> labels) {
        super();
        this.fileName = fileName;
        this.pieces = pieces;
        this.paragraphs = paragraphs;
        this.labels = labels;
    }

    public ScenarioPiece(String name) {
        super();
        this.fileName = name;
        this.pieces = new ArrayList<>();
        paragraphs = new int[0];
        labels = new HashMap<>();
    }
    
}
