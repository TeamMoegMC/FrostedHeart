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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;

import net.minecraft.item.Item.Properties;

public class GeneratorUpgraderI extends FHBaseItem {
    IETemplateMultiblock ietm = FHMultiblocks.GENERATOR_T2;

    public GeneratorUpgraderI(Properties properties) {
        super(properties);
    }

    public boolean createStructure(PlayerEntity entityplayer, World worldIn) {
        if (entityplayer instanceof ServerPlayerEntity) {
            BlockRayTraceResult brtr = getPlayerPOVHitResult(worldIn, entityplayer, FluidMode.ANY);
            if (brtr.getType() == Type.MISS) return false;
            if (brtr.getDirection().getStepY() != 0) return false;
            TileEntity te = Utils.getExistingTileEntity(worldIn, brtr.getBlockPos().relative(brtr.getDirection().getOpposite(), 1));
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
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 400;
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        playerIn.startUsingItem(handIn);
        if (playerIn instanceof ServerPlayerEntity && playerIn.abilities.instabuild) {
            createStructure(playerIn, worldIn);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getItemInHand(handIn));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (worldIn.isClientSide) return stack;
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        createStructure(entityplayer, worldIn);
        return stack;
    }
}
