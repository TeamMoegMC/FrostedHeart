/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.machines;

import java.util.List;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.research.Research;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class RubbingTool extends FHBaseItem {

    public RubbingTool(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        playerIn.setActiveHand(handIn);
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (worldIn.isRemote) return stack;
        if (stack.getDamage() >= stack.getMaxDamage()) return stack;
        if (!hasResearch(stack)) return stack;
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        if (entityplayer instanceof ServerPlayerEntity) {
            BlockRayTraceResult brtr = rayTrace(worldIn, entityplayer, FluidMode.NONE);
            if (brtr.getType() == Type.MISS) return stack;

            TileEntity te = Utils.getExistingTileEntity(worldIn, brtr.getPos());
            if (te instanceof MechCalcTileEntity) {
                MechCalcTileEntity mcte = (MechCalcTileEntity) te;
                int crp = mcte.currentPoints;
                mcte.currentPoints = 0;
                mcte.updatePoints();
                if (crp > 0) {
                    stack.setDamage(stack.getDamage() + 1);
                    crp += getPoint(stack);
                    setPoint(stack, crp);
                }
            }
        }
        return stack;
    }

    public static int getPoint(ItemStack stack) {
        return stack.getOrCreateTag().getInt("points");
    }

    public static void setPoint(ItemStack stack, int val) {
        if (val <= 0)
            stack.getOrCreateTag().remove("points");
        else
            stack.getOrCreateTag().putInt("points", val);
    }

    public static void setResearch(ItemStack stack, String rs) {
        if (rs == null)
            stack.getOrCreateTag().remove("research");
        else
            stack.getOrCreateTag().putString("research", rs);
    }

    public static boolean hasResearch(ItemStack stack) {
        return stack.getOrCreateTag().contains("research");
    }

    public static String getResearch(ItemStack stack) {
        return stack.getOrCreateTag().getString("research");
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (hasResearch(stack)) {
            Research rs = FHResearch.getResearch(getResearch(stack)).get();
            if (rs != null)
                tooltip.add(GuiUtils.translateTooltip("rubbing.current", rs.getName()).mergeStyle(TextFormatting.GOLD));
            else
                tooltip.add(GuiUtils.translateTooltip("rubbing.current.empty").mergeStyle(TextFormatting.GRAY));
        } else
            tooltip.add(GuiUtils.translateTooltip("rubbing.current.empty").mergeStyle(TextFormatting.GRAY));
        int points = getPoint(stack);
        if (points > 0) {
            tooltip.add(GuiUtils.translateTooltip("rubbing.points", points));
            tooltip.add(GuiUtils.translateTooltip("rubbing.points.hint").mergeStyle(TextFormatting.YELLOW));
        }
        super.addInformation(stack, worldIn, tooltip, flagIn);

    }

}
