package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.content.recipes.RecipeInnerDismantle;
import com.teammoeg.frostedheart.util.FHUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(at=@At(value="INVOKE",
			target="Lnet/minecraft/item/ItemStack;shrink(I)V",
			ordinal=0),method="damageItem")
	public  void FH$InnerItemBreak(int amount,LivingEntity  entityIn, Consumer onBroken,CallbackInfo cbi) {
		ItemStack item=RecipeInnerDismantle.tryDismantle((ItemStack)(Object)this);
		if(!item.isEmpty()&&entityIn instanceof PlayerEntity)
			FHUtils.giveItem((PlayerEntity) entityIn, item);
	}
}
