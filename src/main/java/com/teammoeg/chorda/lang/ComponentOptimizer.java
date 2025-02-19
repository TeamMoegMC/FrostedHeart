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

package com.teammoeg.chorda.lang;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
/**
 * A convenience method to merge style-char pair to style-string pair, merging items with same style, optimize network or storage.
 * */
public class ComponentOptimizer {
	StringBuilder sb=new StringBuilder();
	Style sty=Style.EMPTY;
	List<Component> calculated=new ArrayList<>();
	public ComponentOptimizer() {
		
	}
	public void appendChar(String ch,Style style) {
		if(style!=sty) {
			createComponent();
			sty=style;
		}
		sb.append(ch);
	}
	public void createComponent() {
		if(sb.length()!=0) {
			calculated.add(Components.str(sb.toString()).withStyle(sty));
			sb=new StringBuilder();
		}
		sty=Style.EMPTY;
		
	}
	public Component build() {
		createComponent();
		MutableComponent mstr= Components.str("");
		for(Component c:calculated) {
			mstr.append(c);
		}
		return mstr;
	}
	public void appendChar(char ch, Style style) {
		if(style!=sty) {
			createComponent();
			sty=style;
		}
		sb.append(ch);
	}
	public void appendComponent(Component c) {
		createComponent();
		calculated.add(c);
	}
}
