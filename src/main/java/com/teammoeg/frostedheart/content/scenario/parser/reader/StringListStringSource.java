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

package com.teammoeg.frostedheart.content.scenario.parser.reader;

import java.util.List;

public class StringListStringSource implements CodeLineSource {
	int idx=0;
	String name;
	List<String> strs;
	public StringListStringSource(String name, List<String> strs) {
		super();
		this.name = name;
		this.strs = strs;
	}
	
	@Override
	public boolean hasNext() {
		return idx<strs.size();
	}

	@Override
	public String read() {
		return strs.get(idx++);
	}

	@Override
	public String getName() {
		return name;
	}

}
