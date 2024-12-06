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

package com.teammoeg.frostedheart.content.climate.food;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.FHDamageTypes;
import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.water.item.DrinkContainerItem;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.List;

public class FoodTemperatureHandler {
    public static final String TAG_FOOD_TEMPERATURE = "Temperature";
    public static final byte FROZEN = 0;
    public static final byte COLD = 1;
    public static final byte HOT = 2;
    public static final int UPDATE_INTERVAL_TICKS = 200;
    public static final double UPDATE_CHANCE = 1.0;
    public static final float HOT_FOOD_EAT_DURATION_MODIFIER = 0.8F;
    public static final float COLD_FOOD_EAT_DURATION_MODIFIER = 2;
    public static final float DEFAULT_COLD_FOOD_HEAT = -0.5F;
    public static final float DEFAULT_HOT_FOOD_HEAT = 0.5F;

    // Called in FHPlayerEvents
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            Inventory inv = player.getInventory();
            if (player.tickCount % UPDATE_INTERVAL_TICKS != 0)
                return;
            if (player.getRandom().nextFloat() < UPDATE_CHANCE)
                return;
            if (player.isCreative() || player.isSpectator())
                return;
            if (inv.isEmpty())
                return;

//            float posTemp = ChunkHeatData.getTemperature(player.level(), player.blockPosition());
            float envTemp = PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getEnvTemp).orElse(0F);

            final List<NonNullList<ItemStack>> compartments = ImmutableList.of(inv.items, inv.armor, inv.offhand);
            for(NonNullList<ItemStack> nonnulllist : compartments) {
                for(int i = 0; i < nonnulllist.size(); ++i) {
                    if (!nonnulllist.get(i).isEmpty()) {
                        ItemStack stack = nonnulllist.get(i);
                        if (canChangeTemperatureFood(stack)) {
                            byte temperature = FoodTemperatureHandler.getTemperature(stack);
                            // decrease temp
                            if (envTemp < WorldTemperature.FOOD_FROZEN_TEMPERATURE) {
                                if (temperature == FoodTemperatureHandler.COLD) {
                                    FoodTemperatureHandler.setTemperature(stack, FoodTemperatureHandler.FROZEN);
                                } else if (temperature == FoodTemperatureHandler.HOT) {
                                    FoodTemperatureHandler.setTemperature(stack, FoodTemperatureHandler.COLD);
                                }
                            }
                            // when hot, become normal, when normal, stays, when frozen, become normal
                            else if (temperature != FoodTemperatureHandler.COLD) {
                                FoodTemperatureHandler.setTemperature(stack, FoodTemperatureHandler.COLD);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void checkFoodBeforeEating(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ItemStack stack = event.getItem();

        if (canChangeTemperatureFood(stack)) {
            byte temperature = FoodTemperatureHandler.getTemperature(stack);
            if (temperature == FoodTemperatureHandler.COLD) {
                event.setDuration((int) (event.getDuration() * COLD_FOOD_EAT_DURATION_MODIFIER));
            } else if (temperature == FoodTemperatureHandler.FROZEN) {
                // cannot eat frozen food
                player.displayClientMessage(Lang.translateMessage("food.frozen"), true);
                event.setCanceled(true);
            } else if (temperature == FoodTemperatureHandler.HOT) {
                event.setDuration((int) (event.getDuration() * HOT_FOOD_EAT_DURATION_MODIFIER));
            }
        }
    }

    // after eating
    public static void checkFoodAfterEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer))
            return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        ItemStack stack = event.getItem();
        Item it = stack.getItem();

        ITempAdjustFood adj;
        double tspeed = FHConfig.SERVER.tempSpeed.get();
        if (it instanceof ITempAdjustFood) {
            adj = (ITempAdjustFood) it;
        } else {
            adj = FHDataManager.getTempAdjustFood(stack);
        }

        // Depending on food temperature status, display message, and set default heat, which could be overridden if
        // the food is a ITempAdjustFood
        float max = 15F;
        float min = -15F;
        float heat = 0F;
        if (isFoodOrDrink(stack)) {
            byte temperature = FoodTemperatureHandler.getTemperature(stack);
            if (temperature == FoodTemperatureHandler.COLD) {
                player.displayClientMessage(Lang.translateMessage("food.cold"), true);
                heat = DEFAULT_COLD_FOOD_HEAT;
            } else if (temperature == FoodTemperatureHandler.HOT) {
                player.displayClientMessage(Lang.translateMessage("food.hot"), true);
                heat = DEFAULT_HOT_FOOD_HEAT;
            }

            // Get the current body temperature
            float current = PlayerTemperatureData.getCapability((ServerPlayer) event.getEntity()).map(PlayerTemperatureData::getBodyTemp).orElse(0f);

            // Fetch data from ITempAdjustFood, if available. Otherwise, use default values.
            if (adj != null) {
                max = adj.getMaxTemp(event.getItem());
                min = adj.getMinTemp(event.getItem());
                heat = adj.getHeat(event.getItem(),PlayerTemperatureData.getCapability((ServerPlayer) event.getEntity()).map(PlayerTemperatureData::getEnvTemp).orElse(0f));
            }

            // Adjust body temperature
            if (heat > 1) {
                event.getEntity().hurt(FHDamageTypes.createSource(event.getEntity().level(), FHDamageTypes.HYPERTHERMIA_INSTANT, event.getEntity()), (heat) * 2);
            } else if (heat < -1)
                event.getEntity().hurt(FHDamageTypes.createSource(event.getEntity().level(), FHDamageTypes.HYPOTHERMIA_INSTANT, event.getEntity()), (heat) * 2);
            if (heat > 0) {
                if (current >= max)
                    return;
                current += (float) (heat * tspeed);
                if (current > max)
                    current = max;
            } else {
                if (current <= min)
                    return;
                current += (float) (heat * tspeed);
                if (current <= min)
                    return;
            }

            // Set body temperature
            final float toset = current;
            PlayerTemperatureData.getCapability((ServerPlayer) event.getEntity()).ifPresent(t->t.setBodyTemp(toset));
        }
    }

    // Helpers

    public static void setTemperature(ItemStack stack, byte temperature) {
        stack.getOrCreateTag().putInt(TAG_FOOD_TEMPERATURE, Math.max(0, temperature));
    }

    public static byte getTemperature(ItemStack stack) {
        if (!stack.hasTag() || stack.hasTag() && !stack.getTag().contains(TAG_FOOD_TEMPERATURE)) {
            return -1;
        }
        return stack.getTag().getByte(TAG_FOOD_TEMPERATURE);
    }

    /**
     * Can be eaten OR drunk, AND not dry AND not insulated.
     * @param stack Food
     * @return whether this food can change temperature in inventory.
     */
    public static boolean canChangeTemperatureFood(ItemStack stack) {
        return (stack.getItem().isEdible() ||
                stack.getItem() instanceof DrinkContainerItem && ((DrinkContainerItem) stack.getItem()).isDrinkable(stack))
                && !stack.is(FHTags.Items.DRY_FOOD.tag)
                && !(stack.is(FHTags.Items.INSULATED_FOOD.tag));
    }

    public static boolean isFoodOrDrink(ItemStack stack) {
        return stack.getItem().isEdible() ||
                stack.getItem() instanceof DrinkContainerItem && ((DrinkContainerItem) stack.getItem()).isDrinkable(stack);
    }
}
