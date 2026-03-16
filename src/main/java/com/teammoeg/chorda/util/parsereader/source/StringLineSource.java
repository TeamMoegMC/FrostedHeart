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

/**
 * 基于字符串的行数据源实现，从单个字符串中逐字符读取。
 * <p>
 * String-based line source implementation that reads character by character from a single string.
 */
public class StringLineSource extends StreamLineSource {
	String code;
	int idx;
	/**
	 * 构造字符串行数据源。
	 * <p>
	 * Construct a string line source.
	 *
	 * @param name 数据源名称 / the source name
	 * @param code 源代码字符串 / the source code string
	 */
	public StringLineSource(String name, String code) {
		super(name);
		this.code = code;
	}

	/**
	 * {@inheritDoc}
	 * 从源代码字符串中读取下一个字符码点。到达字符串末尾时返回-1。
	 * <p>
	 * Reads the next character code point from the source code string. Returns -1 when the end of the string is reached.
	 */
	@Override
	public int readCh() {
		if(idx<code.length())
			return code.codePointAt(idx++);
		return -1;
	}

}
