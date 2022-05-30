package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public class EffectCrafting extends Effect{
    public EffectCrafting(ItemStack item) {
    	super(GuiUtils.translateGui("effect.crafting"),new ArrayList<>(),item);
    	tooltip.add(new TranslationTextComponent(item.getTranslationKey()));
    }

    public EffectCrafting(JsonObject jo) {
    	super(jo);
    }
    public EffectCrafting(IItemProvider item) {
    	this(new ItemStack(item));
	}

	@Override
    public void init() {

    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }

	@Override
	public ResourceLocation getId() {
		return null;
	}
}
