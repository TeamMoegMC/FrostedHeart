/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.minecraft.research;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaBlock;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Mark machine owner for research
 * <p>
 * */
@SuppressWarnings("unused")
@Mixin({MechanicalCrafterBlock.class, SaunaBlock.class})
public class OwnerBlockMixin_Research extends Block {


    public OwnerBlockMixin_Research(Properties properties) {
        super(properties);
    }

    @Inject(at = @At("HEAD"), method = "use")
    public void fh$use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,BlockHitResult hit, CallbackInfoReturnable<InteractionResult> r) {
        if (!worldIn.isClientSide && !(player instanceof FakePlayer)) {
            BlockEntity te = Utils.getExistingTileEntity(worldIn, pos);
            IOwnerTile.trySetOwner(te, FHTeamDataManager.get(player).getId());
        }
    }

    //@Inject(at=@At("HEAD"),method="onBlockPlacedBy(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V")
    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);

        if (placer instanceof ServerPlayer && !(placer instanceof FakePlayer))
            IOwnerTile.trySetOwner(Utils.getExistingTileEntity(worldIn, pos), FHTeamDataManager.get((Player) placer).getId());
    }
}
