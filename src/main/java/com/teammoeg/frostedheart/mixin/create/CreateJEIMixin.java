package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

@Mixin(targets="com.simibubi.create.compat.jei.CreateJEI$CategoryBuilder",remap=false)
public class CreateJEIMixin {
	@Shadow(remap=false)
	private CreateRecipeCategory category;
	@Inject(at=@At("HEAD"),method="build",remap=false,cancellable=true)
	private void build(CallbackInfoReturnable<CreateRecipeCategory> cat) {
		if(category.getUid().getPath().equals("automatic_shapeless"))cat.setReturnValue(category);
	}
}
