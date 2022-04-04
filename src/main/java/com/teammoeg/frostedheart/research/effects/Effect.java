package com.teammoeg.frostedheart.research.effects;

import java.util.List;

import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * "Effect" of an research: how would it becomes when a research is completed ?
 * 
 * */
<<<<<<< HEAD
public abstract class Effect implements Writeable{
	ITextComponent name;
	List<ITextComponent> tooltip;
=======
public abstract class Effect {
	IFormattableTextComponent name;
	List<IFormattableTextComponent> tooltip;
>>>>>>> refs/remotes/origin/master
	ItemStack icon;
	public abstract void init();
	public abstract void grant();
	public abstract void revoke();

	public ItemStack getIcon() {
		return icon;
	}

	public IFormattableTextComponent getName() {
		return name;
	}

	public List<IFormattableTextComponent> getTooltip() {
		return tooltip;
	}
}
