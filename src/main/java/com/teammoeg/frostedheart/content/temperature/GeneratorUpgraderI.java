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

package com.teammoeg.frostedheart.content.temperature;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorTileEntity;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.mixin.MultiBlockAccess;

import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
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
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;

public class GeneratorUpgraderI extends FHBaseItem {
    IETemplateMultiblock ietm = FHMultiblocks.GENERATOR_T2;

    public GeneratorUpgraderI(String name, Properties properties) {
        super(name, properties);
    }

    public boolean createStructure(PlayerEntity entityplayer, World worldIn) {
        if (entityplayer instanceof ServerPlayerEntity) {
            BlockRayTraceResult brtr = rayTrace(worldIn, entityplayer, FluidMode.ANY);
            if (brtr.getType() == Type.MISS) return false;
            if (brtr.getFace().getYOffset() != 0) return false;
            TileEntity te = Utils.getExistingTileEntity(worldIn, brtr.getPos().offset(brtr.getFace().getOpposite(), 1));
            System.out.println(te);
            if (!(te instanceof T1GeneratorTileEntity)) return false;
            T1GeneratorTileEntity t1te = (T1GeneratorTileEntity) te;
            System.out.println(t1te);
            if (t1te.isDummy()) return false;
            Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, brtr.getFace().getOpposite());
            ((MultiBlockAccess) (Object) ietm).callForm(worldIn, brtr.getPos().offset(Direction.DOWN).offset(brtr.getFace().rotateY()).offset(brtr.getFace().getOpposite(), 2), rot, Mirror.NONE, brtr.getFace());
            TileEntity nte = Utils.getExistingTileEntity(worldIn, brtr.getPos().offset(brtr.getFace(), 1));
            System.out.println(nte);
            if (nte instanceof T2GeneratorTileEntity) {//bug: cannot get correct te
                ((T2GeneratorTileEntity) nte).setWorking(t1te.isWorking());
                ((T2GeneratorTileEntity) nte).setOverdrive(t1te.isOverdrive());
                ((T2GeneratorTileEntity) nte).setOwner(ResearchDataAPI.getData(entityplayer).getId());
                NonNullList<ItemStack> nnl = ((T2GeneratorTileEntity) nte).getInventory();
                for (int i = 0; i < nnl.size(); i++) {
                    ((T2GeneratorTileEntity) nte).getInventory().set(i, nnl.get(i));
                }
                ((T2GeneratorTileEntity) nte).process = t1te.process;
                ((T2GeneratorTileEntity) nte).processMax = t1te.processMax;
            }
            return true;
        }
        return true;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 400;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        playerIn.setActiveHand(handIn);
        if (playerIn instanceof ServerPlayerEntity && playerIn.abilities.isCreativeMode) {
            createStructure(playerIn, worldIn);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (worldIn.isRemote) return stack;
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        createStructure(entityplayer, worldIn);
        return stack;
    }
}
