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

public abstract class StreamLineSource implements CodeLineSource {
	String name;
	boolean hasNext=true;
	int lch=0;
	public StreamLineSource(String name) {
		super();
		this.name = name;
	}

	public abstract int readCh();

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

	@Override
	public String getName() {
		return name;
	}

	@Override
	public final boolean hasNext() {
		return hasNext;
	}

}
