package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import dev.ftb.mods.ftblibrary.ui.GuiHelper;


@Mixin(GuiHelper.class)
public class GuiHelperMixin {
	@ModifyConstant(method="drawItem",constant=@Constant(intValue=0xF000F0),remap=false)
	private static int setLightValue(int cnst) {
		return 0xFFFFFF;
	}
}
