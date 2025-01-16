package com.teammoeg.chorda.util.lang;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

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
}
