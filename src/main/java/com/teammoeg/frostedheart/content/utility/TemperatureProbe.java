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

package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper.toTemperatureFloatString;

public class TemperatureProbe extends Item {
    private static final List<Predicate<Player>> IS_WEARING_PREDICATES = new ArrayList<>();

    static {
        addIsWearingPredicate(player -> FHItems.temperatureProbe.isIn(player.getItemBySlot(EquipmentSlot.OFFHAND)) ||
                FHItems.temperatureProbe.isIn(player.getItemBySlot(EquipmentSlot.MAINHAND)));
    }

    public TemperatureProbe(Properties properties) {
        super(properties);
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
        return 100;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        playerIn.displayClientMessage(Lang.translateMessage("thermometer.testing"), true);
        playerIn.startUsingItem(handIn);
        if (playerIn instanceof ServerPlayer && playerIn.getAbilities().instabuild) {
            BlockHitResult brtr = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.ANY);
            if (brtr.getType() != HitResult.Type.MISS) {

                playerIn.sendSystemMessage(Lang.translateMessage("info.air_temperature", toTemperatureFloatString(WorldTemperature.air(playerIn.level(), brtr.getBlockPos()))));
            }

        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        if (worldIn.isClientSide) return stack;
        Player entityplayer = entityLiving instanceof Player ? (Player) entityLiving : null;
        if (entityplayer instanceof ServerPlayer) {
            BlockHitResult brtr = getPlayerPOVHitResult(worldIn, entityplayer, ClipContext.Fluid.ANY);
            if (brtr.getType() == HitResult.Type.MISS) return stack;
            entityplayer.sendSystemMessage(Lang.translateMessage("info.air_temperature", toTemperatureFloatString(WorldTemperature.air(entityplayer.level(), brtr.getBlockPos()))));
        }
        return stack;
    }

    public static boolean isWearingTemperatureProbe(Player player) {
        for (Predicate<Player> predicate : IS_WEARING_PREDICATES) {
            if (predicate.test(player)) {
                return true;
            }
        }
        return false;
    }

    public static void addIsWearingPredicate(Predicate<Player> predicate) {
        IS_WEARING_PREDICATES.add(predicate);
    }
}
