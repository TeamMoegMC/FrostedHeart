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
import java.util.Optional;

import com.mojang.datafixers.util.Unit;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText.ContentConsumer;
import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
/**
 * A convenience method to merge style-char pair to style-string pair, merging items with same style, optimize network or storage.
 * */
public class ComponentOptimizer implements ContentConsumer<Unit>,StyledContentConsumer<Unit>{
	StringBuilder sb=new StringBuilder();
	Style sty=Style.EMPTY;
	List<Component> calculated=new ArrayList<>();
	public ComponentOptimizer() {
		
	}
	public static MutableComponent optimize(Component o) {
		ComponentOptimizer com=new ComponentOptimizer();
		o.visit(com, Style.EMPTY);
		return com.build();
	}
	public void appendChar(String ch,Style style) {
		if(style!=sty&&!style.equals(sty)) {
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
	public MutableComponent build() {
		createComponent();
		MutableComponent mstr= Components.str("");
		for(Component c:calculated) {
			mstr.append(c);
		}
		return mstr;
	}
	public void appendChar(char ch, Style style) {
		if(style!=sty&&!style.equals(sty)) {
			createComponent();
			sty=style;
		}
		sb.append(ch);
	}
	public void appendRawComponent(Component c) {
		createComponent();
		calculated.add(c);
	}
	@Override
	public Optional<Unit> accept(Style pStyle, String pContent) {
		appendChar(pContent,sty.applyTo(pStyle));
		return Optional.empty();
	}
	@Override
	public Optional<Unit> accept(String pContent) {
		appendChar(pContent,sty);
		return Optional.empty();
	}
}
