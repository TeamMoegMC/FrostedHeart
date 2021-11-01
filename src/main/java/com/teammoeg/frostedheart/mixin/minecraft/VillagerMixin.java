package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

@Mixin(VillagerEntity.class)
public abstract class VillagerMixin extends AbstractVillagerEntity {
	public VillagerMixin(EntityType<? extends AbstractVillagerEntity> type, World worldIn) {
		super(type, worldIn);
	}
	@Shadow
	protected abstract void shakeHead();
	/**
	 * @author khjxiaogu
	 * @reason disable villager trade for our system
	 */
	@Overwrite
	private void displayMerchantGui(PlayerEntity player) {
		this.shakeHead();
		player.sendStatusMessage(GuiUtils.translateMessage("village.unknown"), false);
	}
}
