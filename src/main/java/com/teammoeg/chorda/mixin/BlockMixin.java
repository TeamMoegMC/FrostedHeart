package com.teammoeg.chorda.mixin;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.chorda.events.BlockDropsEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;

@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviour {

	public BlockMixin(Properties pProperties) {
		super(pProperties);
		// TODO Auto-generated constructor stub
	}

	@Inject(at = @At("RETURN"), method = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",cancellable=true,allow=1,require=1,expect=1,remap=true)
	private static void chorda$getDrops(BlockState pState, ServerLevel pLevel, BlockPos pPos, @Nullable BlockEntity pBlockEntity, @Nullable Entity pEntity, ItemStack pTool,
		CallbackInfoReturnable<List<ItemStack>> ret) {
		BlockDropsEvent ev = new BlockDropsEvent(pState, pLevel, pPos, pBlockEntity, pEntity, pTool, ret.getReturnValue());
		if(MinecraftForge.EVENT_BUS.post(ev))
			ret.setReturnValue(new ArrayList<>());
		ret.setReturnValue(ev.getDrops());
	}
}
