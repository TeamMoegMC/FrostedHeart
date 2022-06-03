package com.teammoeg.frostedheart.research.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHMain;

import dev.ftb.mods.ftblibrary.util.ClientTextComponentUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class FHTextUtil {

	private FHTextUtil() {
	}
	public static ITextComponent get(String orig,String type,Supplier<String> pid) {
		if(orig.length()==0)return new TranslationTextComponent(type+"."+FHMain.MODID+"."+pid.get());
		if(orig.startsWith("@")) {
			if(orig.length()==1)return new TranslationTextComponent(pid.get());
			return new TranslationTextComponent(orig.substring(1));
		}
		return ClientTextComponentUtils.parse(orig);
	}
	public static List<ITextComponent> get(List<String> orig,String type,Supplier<String> pid) {
		String s=pid.get();
		List<ITextComponent> li=new ArrayList<>();
		for(int i=0;i<orig.size();i++) {
			final int fi=i;
			li.add(get(orig.get(i),type,()->s+"."+fi));
		}
		return li;
	}
}
