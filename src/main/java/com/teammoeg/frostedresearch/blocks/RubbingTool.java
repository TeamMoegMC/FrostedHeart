/*
 * Copyright (c) 2026 TeamMoeg
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
import java.util.UUID;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;

public class RubbingTool extends FRBaseItem{

    public RubbingTool(Properties properties) {
        super(properties);
    }

    /** Returns the bound Chorda team UUID, or null for legacy/unbound tools. */
    public static UUID getTeam(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("team") ? stack.getTag().getUUID("team") : null;
    }

    /** Binds an empty tool to a research and its receiving team in one operation. */
    public static void bind(ItemStack stack, String research, UUID team) {
        setResearch(stack, research);
        stack.getOrCreateTag().putUUID("team", team);
    }

    /** Checks the bound team's research eligibility independently of the operator. */
    public static boolean canCollect(ItemStack stack) {
        UUID team = getTeam(stack);
        Research research = FHResearch.getResearch(getResearch(stack));
        return team != null && research != null && ResearchDataAPI.getData(team)
                .map(data -> data.get().getData(research).canResearch()
                        && !data.get().getData(research).isCompleted()).orElse(false);
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
        UUID team = getTeam(stack);
        if (team != null) tooltip.add(Component.literal("队伍: " + team).withStyle(ChatFormatting.GRAY));
        else if (hasResearch(stack) || points > 0)
            tooltip.add(Component.literal(points > 0 ? "旧工具点数来源队伍未知，无法取点或提交" : "请在绘图桌重新绑定研究与队伍")
                    .withStyle(ChatFormatting.YELLOW));
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
        if (!(entityLiving instanceof ServerPlayer player) || !canCollect(stack)) return stack;
        Player entityplayer = entityLiving instanceof Player ? (Player) entityLiving : null;
        if (entityplayer instanceof ServerPlayer) {
            BlockHitResult brtr = getPlayerPOVHitResult(worldIn, entityplayer, Fluid.NONE);
            if (brtr.getType() == Type.MISS) return stack;

            BlockEntity te = CUtils.getExistingTileEntity(worldIn, brtr.getBlockPos());
            if (te instanceof ComputeMachine mcte) {
	            if(mcte.getFetchablePoints(getTeam(stack))>=100) {
	                int crp = mcte.fetchPoint(getTeam(stack), 100);
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
