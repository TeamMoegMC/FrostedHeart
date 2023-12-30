package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.climate.data.DeathInventoryData;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World worldIn) {
		super(type, worldIn);
	}

	@Inject(at = @At("HEAD"), method = "dropInventory")
	public void fh$dropInventory(CallbackInfo cbi) {
		super.dropInventory();
		if (((Object)this) instanceof FakePlayer)
			return;
		DeathInventoryData dit = DeathInventoryData.get((PlayerEntity) ((Object)this));
		dit.startClone();
	}



}
