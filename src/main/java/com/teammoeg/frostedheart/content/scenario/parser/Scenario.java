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

package com.teammoeg.frostedheart.content.scenario.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scenario {
    public final String name;
    public final List<Node> pieces;
    public final int[] paragraphs;
    public final Map<String, Integer> labels;

    public Scenario(String fileName, List<Node> pieces, int[] paragraphs, Map<String, Integer> labels) {
        super();
        this.name = fileName;
        this.pieces = pieces;
        this.paragraphs = paragraphs;
        this.labels = labels;
        /*for(Node n:pieces) {
        	System.out.println(n.getText());
        }*/
    }

    public Scenario(String name) {
        super();
        this.name = name;
        this.pieces = new ArrayList<>();
        paragraphs = new int[0];
        labels = new HashMap<>();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Scenario other = (Scenario) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
    
}
