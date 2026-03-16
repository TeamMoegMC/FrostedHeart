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

package com.teammoeg.chorda.text;

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
 * 组件优化器，将相同样式的字符对合并为样式-字符串对，减少网络传输或存储开销。
 * <p>
 * Component optimizer that merges style-character pairs into style-string pairs,
 * combining items with the same style to optimize network transmission or storage.
 */
public class ComponentOptimizer implements ContentConsumer<Unit>,StyledContentConsumer<Unit>{
	StringBuilder sb=new StringBuilder();
	Style sty=Style.EMPTY;
	List<Component> calculated=new ArrayList<>();
	public ComponentOptimizer() {
		
	}
	/**
	 * 优化给定组件，合并具有相同样式的相邻文本部分。
	 * <p>
	 * Optimizes the given component by merging adjacent text parts with the same style.
	 *
	 * @param o 要优化的组件 / The component to optimize
	 * @return 优化后的可变组件 / The optimized mutable component
	 */
	public static MutableComponent optimize(Component o) {
		ComponentOptimizer com=new ComponentOptimizer();
		o.visit(com, Style.EMPTY);
		return com.build();
	}
	/**
	 * 追加一个字符串，当样式改变时创建新组件。
	 * <p>
	 * Appends a string, creating a new component when the style changes.
	 *
	 * @param ch 要追加的字符串 / The string to append
	 * @param style 字符串的样式 / The style of the string
	 */
	public void appendChar(String ch,Style style) {
		if(style!=sty&&!style.equals(sty)) {
			createComponent();
			sty=style;
		}
		sb.append(ch);
	}
	/**
	 * 将当前缓冲的文本创建为组件并添加到结果列表中。
	 * <p>
	 * Creates a component from the current buffered text and adds it to the result list.
	 */
	public void createComponent() {
		if(sb.length()!=0) {
			calculated.add(Components.str(sb.toString()).withStyle(sty));
			sb=new StringBuilder();
		}
		sty=Style.EMPTY;
		
	}
	/**
	 * 构建最终的优化组件。
	 * <p>
	 * Builds the final optimized component.
	 *
	 * @return 合并后的可变组件 / The merged mutable component
	 */
	public MutableComponent build() {
		createComponent();
		MutableComponent mstr= Components.str("");
		for(Component c:calculated) {
			mstr.append(c);
		}
		return mstr;
	}
	/**
	 * 追加一个字符，当样式改变时创建新组件。
	 * <p>
	 * Appends a character, creating a new component when the style changes.
	 *
	 * @param ch 要追加的字符 / The character to append
	 * @param style 字符的样式 / The style of the character
	 */
	public void appendChar(char ch, Style style) {
		if(style!=sty&&!style.equals(sty)) {
			createComponent();
			sty=style;
		}
		sb.append(ch);
	}
	/**
	 * 追加一个原始组件，不进行合并优化。
	 * <p>
	 * Appends a raw component without merge optimization.
	 *
	 * @param c 要追加的原始组件 / The raw component to append
	 */
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
