/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.blocks;

import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.item.FRBaseItem;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

import java.util.List;

public class RubbingTool extends FRBaseItem{

    public RubbingTool(Properties properties) {
        super(properties);
    }

    public static int getPoint(ItemStack stack) {
        return stack.getOrCreateTag().getInt("points");
    }

    public static String getResearch(ItemStack stack) {
        return stack.getOrCreateTag().getString("research");
    }

    public static boolean hasResearch(ItemStack stack) {
        return stack.getOrCreateTag().contains("research");
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

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (hasResearch(stack)) {
            Research rs = FHResearch.getResearch(getResearch(stack));
            if (rs != null)
                tooltip.add(Lang.translateTooltip("rubbing.current", rs.getName()).withStyle(ChatFormatting.GOLD));
            else
                tooltip.add(Lang.translateTooltip("rubbing.current.empty").withStyle(ChatFormatting.GRAY));
        } else
            tooltip.add(Lang.translateTooltip("rubbing.current.empty").withStyle(ChatFormatting.GRAY));
        int points = getPoint(stack);
        if (points > 0) {
            tooltip.add(Lang.translateTooltip("rubbing.points", points));
            tooltip.add(Lang.translateTooltip("rubbing.points.hint").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        playerIn.startUsingItem(handIn);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        if (worldIn.isClientSide) return stack;
        if (stack.getDamageValue() >= stack.getMaxDamage()) return stack;
        if (!hasResearch(stack)) return stack;
        Player entityplayer = entityLiving instanceof Player ? (Player) entityLiving : null;
        if (entityplayer instanceof ServerPlayer) {
            BlockHitResult brtr = getPlayerPOVHitResult(worldIn, entityplayer, Fluid.NONE);
            if (brtr.getType() == Type.MISS) return stack;

            BlockEntity te = CUtils.getExistingTileEntity(worldIn, brtr.getBlockPos());
            if (te instanceof ComputeMachine mcte) {
            	if(mcte.getFetchablePoints()>=100) {
	                int crp = mcte.fetchPoint(10000);
	                if (crp > 0) {
	                    stack.setDamageValue(stack.getDamageValue() + 1);
	                    crp += getPoint(stack);
	                    setPoint(stack, crp);
	                }
            	}
            }
        }
        return stack;
    }

}
