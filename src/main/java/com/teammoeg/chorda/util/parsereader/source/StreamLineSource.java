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

import com.teammoeg.chorda.util.parsereader.CodeLineSource;

/**
 * 基于字符流的行数据源抽象基类，将字符流逐字符读取并按行分割。
 * 子类需实现{@link #readCh()}方法提供字符数据。
 * <p>
 * Abstract base class for stream-based line sources that reads character streams
 * character by character and splits them into lines.
 * Subclasses must implement {@link #readCh()} to provide character data.
 */
public abstract class StreamLineSource implements CodeLineSource {
	String name;
	boolean hasNext=true;
	int lch=0;
	/**
	 * 构造流行数据源。
	 * <p>
	 * Construct a stream line source.
	 *
	 * @param name 数据源名称 / the source name
	 */
	public StreamLineSource(String name) {
		super();
		this.name = name;
	}

	/**
	 * 读取下一个字符。流结束时返回-1。
	 * <p>
	 * Read the next character. Returns -1 when the stream ends.
	 *
	 * @return 字符码点，或-1表示流结束 / the character code point, or -1 for end of stream
	 */
	public abstract int readCh();

	/**
	 * {@inheritDoc}
	 * 逐字符读取并按换行符分割，处理CR、LF和CRLF格式的换行。
	 * <p>
	 * Reads character by character and splits by line breaks, handling CR, LF and CRLF line endings.
	 */
	@Override
	public String readLine() {
		StringBuilder sb=new StringBuilder();
		int ch;
		while((ch=readCh())>0) {
			if(ch=='\r'||ch=='\n') {
				if(lch==0||lch==ch) {
					lch=ch;
					break;
				}
				continue;
			}
			lch=0;
			sb.appendCodePoint(ch);
		}
		if(ch<0)hasNext=false;
		return sb.toString();
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public final boolean hasNext() {
		return hasNext;
	}

}
