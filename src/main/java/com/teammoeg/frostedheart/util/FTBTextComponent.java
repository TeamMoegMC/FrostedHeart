package com.teammoeg.frostedheart.util;

import dev.ftb.mods.ftblibrary.util.ClientTextComponentUtils;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextComponent;

public class FTBTextComponent extends TextComponent {
	String comp;

	public FTBTextComponent(String comp) {
		super();
		this.comp = comp;
	}
	IFormattableTextComponent it;
	protected IFormattableTextComponent intern() {
		if(it==null)
			it=(IFormattableTextComponent) ClientTextComponentUtils.parse(comp);
		return it;
	}
	@Override
	public TextComponent copyRaw() {
		return new FTBTextComponent(comp);
	}


	@Override
	public IReorderingProcessor func_241878_f() {
		return intern().func_241878_f();
	}



}
