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

package com.teammoeg.frostedheart.util.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.teammoeg.chorda.math.Persentage2FractionHelper;
import com.teammoeg.chorda.text.ComponentOptimizer;
import com.teammoeg.chorda.text.Components;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class FineProgressBarBuilder {
	private static record ProgressElement(int color,int len,String icon){
		
	}
	private static final Style def_style= FHTextIcon.applyFont(Style.EMPTY);
	List<ProgressElement> elm=new ArrayList<>();
	MutableComponent parent= Components.str("");
	Persentage2FractionHelper rounder;
	int len;
	public FineProgressBarBuilder(int length) {
		len=length;
		rounder=new Persentage2FractionHelper(length);
	}
	public FineProgressBarBuilder addElement(int color,String icon,float percent) {
		elm.add(new ProgressElement(color,rounder.getPercentRounded(percent),icon));
		return this;
	}
	public Component build() {
		if(elm.isEmpty()) {
			return Components.str("\uF510\uF513"+"\uF512\uF513".repeat(len-2)+"\uF510").withStyle(def_style);
		}
		int lastIconReminder=0;
		int progressedLength=0;
		List<Integer> iconPoss=new ArrayList<>();
		int total=0;
		for(ProgressElement pe:elm) {
			if(pe.icon!=null) {
				int remlen=(pe.len()-8)/2;
				if(remlen<lastIconReminder)
					remlen=lastIconReminder;
				lastIconReminder=remlen+8-pe.len();
				if(lastIconReminder<0)lastIconReminder=0;
				iconPoss.add(remlen+progressedLength);
				progressedLength+=pe.len();
			}else {
				iconPoss.add(-1);
			}
			total+=pe.len();
			//System.out.println(pe.len);
		}
		Iterator<ProgressElement> celm=elm.iterator();
		ProgressElement cur=celm.next();
		Style cstyle=def_style.withColor(cur.color());
		int offset=0;
		
		//StringBuilder iconBuilder=new StringBuilder();
		ComponentOptimizer co=new ComponentOptimizer();
		for(int i=0;i<len;i++) {
			int iconPos=iconPoss.indexOf(i-8);
			if(iconPos!=-1) {
				//System.out.println("Appending icon "+iconPos);
				co.appendChar("\uF511"+elm.get(iconPos).icon+"\uF513", def_style);
			}
			if(i-offset>=cur.len()) {
				offset+=cur.len();
				if(!celm.hasNext())
					break;
				cur=celm.next();
				cstyle=def_style.withColor(cur.color());
			}
			co.appendChar("\uF510\uF513", cstyle);
		}
		int remainLen=len-total;
		if(remainLen>0) {
			//System.out.println(remainLen);
			co.appendChar("\uF512\uF513".repeat(remainLen-1), def_style);
			co.appendChar("\uF510", def_style);
		}
		return co.build();
	}

}
