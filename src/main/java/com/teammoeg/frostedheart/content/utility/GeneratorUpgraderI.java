/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.util.mixin.MultiBlockAccess;

import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.Item.Properties;

public class GeneratorUpgraderI extends FHBaseItem {
    IETemplateMultiblock ietm = FHMultiblocks.GENERATOR_T2;

    public GeneratorUpgraderI(Properties properties) {
        super(properties);
    }

    public boolean createStructure(Player entityplayer, Level worldIn) {
        if (entityplayer instanceof ServerPlayer) {
            BlockHitResult brtr = getPlayerPOVHitResult(worldIn, entityplayer, Fluid.ANY);
            if (brtr.getType() == Type.MISS) return false;
            if (brtr.getDirection().getStepY() != 0) return false;
            BlockEntity te = Utils.getExistingTileEntity(worldIn, brtr.getBlockPos().relative(brtr.getDirection().getOpposite(), 1));
            System.out.println(te);
            if (!(te instanceof T1GeneratorTileEntity)) return false;
            T1GeneratorTileEntity t1te = (T1GeneratorTileEntity) te;
            System.out.println(t1te);
            if (t1te.isDummy()) return false;
            Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, brtr.getDirection().getOpposite());
            ((MultiBlockAccess) ietm).callForm(worldIn, brtr.getBlockPos().relative(Direction.DOWN).relative(brtr.getDirection().getClockWise()).relative(brtr.getDirection().getOpposite(), 2), rot, Mirror.NONE, brtr.getDirection());
            return true;
        }
        return true;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 400;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        playerIn.startUsingItem(handIn);
        if (playerIn instanceof ServerPlayer && playerIn.abilities.instabuild) {
            createStructure(playerIn, worldIn);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        if (worldIn.isClientSide) return stack;
        Player entityplayer = entityLiving instanceof Player ? (Player) entityLiving : null;
        createStructure(entityplayer, worldIn);
        return stack;
    }
}
