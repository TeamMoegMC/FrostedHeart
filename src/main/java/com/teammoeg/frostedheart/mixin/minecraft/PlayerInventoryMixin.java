package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.UnbreakingEnchantment;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.INameable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements IInventory, INameable {
	@Shadow
	public NonNullList<ItemStack> armorInventory;
	@Shadow
	public PlayerEntity player;

	@Inject(at = @At("RETURN"), method = "func_234563_a_",require=1)
	public void FHINJECT$func_234563_a_(DamageSource p_234563_1_, float p_234563_2_, CallbackInfo cbi) {

	}

}
