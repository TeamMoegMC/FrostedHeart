package com.teammoeg.frostedheart.util.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.teammoeg.frostedheart.util.MultipleRoundHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class FineProgressBarBuilder {
	private static record ProgressElement(int color,int len,String icon){
		
	}
	private static final Style def_style=FHTextIcon.applyFont(Style.EMPTY);
	List<ProgressElement> elm=new ArrayList<>();
	MutableComponent parent=Lang.str("");
	MultipleRoundHelper rounder;
	int len;
	public FineProgressBarBuilder(int length) {
		len=length;
		rounder=new MultipleRoundHelper(length);
	}
	public FineProgressBarBuilder addElement(int color,String icon,float percent) {
		elm.add(new ProgressElement(color,rounder.getPercentRounded(percent),icon));
		return this;
	}
	public List<Component> build() {
		if(elm.isEmpty()) {
			return Arrays.asList(Lang.str("\uF510"+"\uF512".repeat(len-2)+"\uF510").withStyle(def_style),Component.empty());
		}
		List<Component> siblings=new ArrayList<>();
		StringBuilder sb=new StringBuilder();
		int total=0;
		for(ProgressElement pe:elm) {
			siblings.add(Lang.str("\uF510".repeat(pe.len())).withStyle(t->t.withColor(pe.color)));
			total+=pe.len();
			//System.out.println(pe.len);
			int prelen=(pe.len()-8)/2;
			for(int i=0;i<pe.len();i++) {
				if(i==prelen&&pe.icon()!=null) {
					sb.append(pe.icon());
					i+=8;
				}else {
					sb.append("\uF511");
				}
			}
		}

		int remainLen=len-total;
		if(remainLen>0) {
			siblings.add(Lang.str("\uF512".repeat(remainLen-1)+"\uF510"));
		}
		MutableComponent mstr=Lang.str("").withStyle(def_style);
		siblings.forEach(mstr::append);
		return Arrays.asList(mstr,Lang.str(sb.toString()).withStyle(def_style));
	}

}
