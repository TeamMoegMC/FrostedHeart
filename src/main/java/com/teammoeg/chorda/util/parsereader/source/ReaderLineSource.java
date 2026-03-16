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

import java.io.IOException;
import java.io.Reader;

/**
 * 基于Reader的行数据源实现，从Java Reader中读取字符数据。
 * <p>
 * Reader-based line source implementation that reads character data from a Java Reader.
 */
public class ReaderLineSource extends StreamLineSource {
	Reader reader;

	/**
	 * 构造Reader行数据源。
	 * <p>
	 * Construct a reader line source.
	 *
	 * @param name 数据源名称 / the source name
	 * @param reader 字符读取器 / the character reader
	 */
	public ReaderLineSource(String name, Reader reader) {
		super(name);
		this.reader = reader;
	}

	/**
	 * {@inheritDoc}
	 * 从底层{@link Reader}读取单个字符。发生IO异常时打印栈追踪并返回-1。
	 * <p>
	 * Reads a single character from the underlying {@link Reader}. Prints stack trace and returns -1 on IO exception.
	 */
	@Override
	public int readCh() {
		try {
			return reader.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

}
