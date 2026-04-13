package com.teammoeg.chorda.compat.jei;

import com.teammoeg.chorda.CompatModule;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class JEICompat {

	public JEICompat() {
	}

    public static void showJEICategory(ResourceLocation rl) {
    	if(CompatModule.isJeiLoaded())
    		JEIPlugin.showJEICategory(rl);
    }

    public static void showJEIFor(ItemStack stack) {
    	if(CompatModule.isJeiLoaded())
    		JEIPlugin.showJEIFor(stack);
    }

    public static void showJEIUsageFor(ItemStack stack) {
    	if(CompatModule.isJeiLoaded())
    		JEIPlugin.showJEIUsageFor(stack);
    }

}
