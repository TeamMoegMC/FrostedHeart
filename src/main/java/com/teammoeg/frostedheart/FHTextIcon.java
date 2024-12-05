package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class FHTextIcon {
	private static final ResourceLocation iconFont=FHMain.rl("default");
	public static record TextIconType(String code){
		public MutableComponent getIcon() {
			return TranslateUtils.str(code).withStyle(t->t.withFont(iconFont));
		}
		public MutableComponent appendBefore(Component which) {
			return TranslateUtils.str("").append(getIcon()).append(which);
			
		}
		public MutableComponent appendAfter(Component which) {
			return TranslateUtils.str("").append(which).append(getIcon());
		}
	}
	public static final TextIconType thermometer=new TextIconType("\uF500");

}
