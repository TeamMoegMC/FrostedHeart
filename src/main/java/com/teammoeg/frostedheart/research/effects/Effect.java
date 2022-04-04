package com.teammoeg.frostedheart.research.effects;

import java.util.List;

import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
/**
 * "Effect" of an research: how would it becomes when a research is completed ?
 * 
 * */
public abstract class Effect implements Writeable{
	ITextComponent name;
	List<ITextComponent> tooltip;
	ItemStack icon;
	public abstract void init();
	public abstract void grant();
	public abstract void revoke();

	public ItemStack getIcon() {
		return icon;
	}

	public ITextComponent getName() {
		return name;
	}

	public List<ITextComponent> getTooltip() {
		return tooltip;
	}
}
