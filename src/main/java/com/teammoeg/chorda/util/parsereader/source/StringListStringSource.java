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

package com.teammoeg.chorda.util.parsereader.source;

import java.util.List;

import com.teammoeg.chorda.util.parsereader.CodeLineSource;

/**
 * 基于字符串列表的行数据源实现，将列表中的每个字符串作为一行提供。
 * <p>
 * String list-based line source implementation that provides each string in the list as a line.
 */
public class StringListStringSource implements CodeLineSource {
	int idx=0;
	String name;
	List<String> strs;
	/**
	 * 构造字符串列表行数据源。
	 * <p>
	 * Construct a string list line source.
	 *
	 * @param name 数据源名称 / the source name
	 * @param strs 字符串列表 / the string list
	 */
	public StringListStringSource(String name, List<String> strs) {
		super();
		this.name = name;
		this.strs = strs;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean hasNext() {
		return idx<strs.size();
	}

	/** {@inheritDoc} */
	@Override
	public String readLine() {
		return strs.get(idx++);
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

}
