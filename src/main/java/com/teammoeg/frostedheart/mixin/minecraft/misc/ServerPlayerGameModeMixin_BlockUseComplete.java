package com.teammoeg.frostedheart.mixin.minecraft.misc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin_BlockUseComplete {

	public ServerPlayerGameModeMixin_BlockUseComplete() {

	}
	private int nutritionBefore;
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",ordinal=0),method="useItemOn",locals=LocalCapture.CAPTURE_FAILHARD)
	public void fh$useItemOnPre(ServerPlayer pPlayer, Level pLevel, ItemStack pStack, InteractionHand pHand, BlockHitResult pHitResult,CallbackInfoReturnable<InteractionResult> result,BlockPos blockpos,BlockState state) {
		nutritionBefore=pPlayer.getFoodData().getFoodLevel();
	}
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/advancements/critereon/ItemUsedOnLocationTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",ordinal=0),method="useItemOn",locals=LocalCapture.CAPTURE_FAILHARD)
	public void fh$useItemOnPost(ServerPlayer pPlayer, Level pLevel, ItemStack pStack, InteractionHand pHand, BlockHitResult pHitResult,CallbackInfoReturnable<InteractionResult> result,BlockPos blockpos,BlockState state) {
		if(nutritionBefore<pPlayer.getFoodData().getFoodLevel()) {
			NutritionCapability.getCapability(pPlayer).ifPresent(t->t.eat(pPlayer, state.getCloneItemStack(pHitResult, pLevel, blockpos, pPlayer)));
		}
	}
}
