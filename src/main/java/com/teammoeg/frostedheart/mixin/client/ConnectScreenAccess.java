package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;

@Mixin(ConnectScreen.class)
public interface ConnectScreenAccess {

	
	@Accessor
	Screen getParent();

}
