package com.teammoeg.frostedheart.util.lang;

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
		if(style==sty) {
			sb.append(ch);
		}else {
			createComponent();
			sty=style;
		}
	}
	public void createComponent() {
		if(sb.length()!=0) {
			calculated.add(Lang.str(sb.toString()).withStyle(sty));
			sb=new StringBuilder();
		}
		sty=Style.EMPTY;
		
	}
	public Component build() {
		createComponent();
		MutableComponent mstr=Lang.str("");
		for(Component c:calculated) {
			mstr.append(c);
		}
		return mstr;
	}
}
