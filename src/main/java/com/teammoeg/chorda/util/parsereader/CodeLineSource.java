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

package com.teammoeg.chorda.util.parsereader;

/**
 * 代码行数据源接口，为ParseReader提供逐行读取能力。
 * 实现类负责从不同来源（字符串、流、文件等）按行提供文本数据。
 * <p>
 * Code line source interface providing line-by-line reading capability for ParseReader.
 * Implementations are responsible for providing text data line by line from various sources
 * (strings, streams, files, etc.).
 */
public interface CodeLineSource {
	/**
	 * 检查是否还有下一行可读。
	 * <p>
	 * Check if there is a next line available to read.
	 *
	 * @return 如果还有行可读返回true / true if there are more lines to read
	 */
	boolean hasNext();

	/**
	 * 读取下一行文本。
	 * <p>
	 * Read the next line of text.
	 *
	 * @return 下一行文本内容 / the next line of text
	 */
	String readLine();

	/**
	 * 获取数据源的名称，通常为文件名，用于调试信息。
	 * <p>
	 * Get the name of this source, typically a filename, used for debug information.
	 *
	 * @return 数据源名称 / the source name
	 */
	String getName();
}
