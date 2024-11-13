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

package com.teammoeg.frostedheart.events;


import com.teammoeg.frostedheart.*;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.foods.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {
    /*public static void onRC(PlayerInteractEvent.RightClickItem rci) {
        if (!rci.getWorld().isClientSide
                && RegistryUtils.getRegistryName(rci.getItemStack().getItem()).getNamespace().equals("projecte")) {
            rci.setCancellationResult(InteractionResult.SUCCESS);
            rci.setCanceled(true);
            Level world = rci.getWorld();
            Player player = rci.getPlayer();
            BlockPos pos = rci.getPos();
            ServerLevel serverWorld = (ServerLevel) world;
            ServerPlayer serverPlayerEntity = (ServerPlayer) player;

            serverPlayerEntity.addEffect(
                    new MobEffectInstance(MobEffects.BLINDNESS, (int) (100 * (world.random.nextDouble() + 0.5)), 3));
            serverPlayerEntity.addEffect(
                    new MobEffectInstance(MobEffects.CONFUSION, (int) (1000 * (world.random.nextDouble() + 0.5)), 5));

            serverPlayerEntity.connection.send(
                    new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.TITLE, TranslateUtils.translateMessage("too_cold_to_transmute")));
            serverPlayerEntity.connection.send(
                    new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.SUBTITLE, TranslateUtils.translateMessage("magical_backslash")));

            double posX = pos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * 4.5D;
            double posY = pos.getY() + world.random.nextInt(3) - 1;
            double posZ = pos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * 4.5D;
            if (world.noCollision(EntityType.WITCH.getAABB(posX, posY, posZ))
                    && SpawnPlacements.checkSpawnRules(EntityType.WITCH, serverWorld, MobSpawnType.NATURAL,
                    new BlockPos(posX, posY, posZ), world.getRandom())) {
                FHUtils.spawnMob(serverWorld, new BlockPos(posX, posY, posZ), new CompoundTag(),
                        new ResourceLocation("minecraft", "witch"));
            }
        }
    }*/


    @SubscribeEvent
    public static void sendForecastMessages(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) event.player;
            boolean configAllows = FHConfig.COMMON.enablesTemperatureForecast.get();
            if (configAllows && ResearchDataAPI.getVariants(serverPlayer).getDouble("has_forecast")>0) {
                // Blizzard warning
                //float thisHour = WorldClimate.getTemp(serverPlayer.world);
                boolean thisHourB = WorldClimate.isBlizzard(serverPlayer.level());
                //float nextHour = WorldClimate.getFutureTemp(serverPlayer.world, 1);
                boolean nextHourB = WorldClimate.isFutureBlizzard(serverPlayer.level(), 1);
                if (!thisHourB) { // not in blizzard yet
                    if (nextHourB) {
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_warning")
                                .withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD), true);
                        // serverPlayer.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
                        // GuiUtils.translateMessage("forecast.blizzard_warning")));
                    }
                } else { // in blizzard now
                    if (!nextHourB) {
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_retreating")
                                .withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD), true);
                        // serverPlayer.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
                        // GuiUtils.translateMessage("forecast.blizzard_retreating")));
                    }
                }

                // Morning forecast wakeup time
                if (serverPlayer.level().getDayTime() % 24000 == 40) {
                    float morningTemp = Math.round(WorldClimate.getTemp(serverPlayer.level()) * 10) / 10.0F;
                    float noonTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 6) * 10) / 10.0F;
                    float nightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 12) * 10) / 10.0F;
                    float midnightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 18) * 10) / 10.0F;
                    float tomorrowMorningTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 1, 0) * 10) / 10.0F;
                    TemperatureDisplayHelper.sendTemperatureStatus(serverPlayer, "forecast.morning", false, morningTemp-10, noonTemp-10,
                            nightTemp-10, midnightTemp-10, tomorrowMorningTemp-10);
                    boolean snow = morningTemp < WorldTemperature.SNOW_TEMPERATURE
                            || noonTemp < WorldTemperature.SNOW_TEMPERATURE || nightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || midnightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowMorningTemp < WorldTemperature.SNOW_TEMPERATURE;
                    boolean blizzard = WorldClimate.isBlizzard(serverPlayer.level())
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 6)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 12)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 18)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 1, 0);
                    if (blizzard)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_today"), false);
                    else if (snow)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.snow_today"), false);
                    else
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.clear_today"), false);

                }

                // Night forecast bedtime
                if (serverPlayer.level().getDayTime() % 24000 == 12542) {
                    float nightTemp = Math.round(WorldClimate.getTemp(serverPlayer.level()) * 10) / 10.0F;
                    float midnightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 6) * 10) / 10.0F;
                    float tomorrowMorningTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 12) * 10)
                            / 10.0F;
                    float tomorrowNoonTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 18) * 10)
                            / 10.0F;
                    float tomorrowNightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 1, 0) * 10)
                            / 10.0F;
                    TemperatureDisplayHelper.sendTemperatureStatus(serverPlayer, "forecast.night", false, nightTemp-10, midnightTemp-10,
                            tomorrowMorningTemp-10, tomorrowNoonTemp-10, tomorrowNightTemp-10);
                    boolean snow = nightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || midnightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowMorningTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowNoonTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowNightTemp < WorldTemperature.SNOW_TEMPERATURE;
                    boolean blizzard = WorldClimate.isBlizzard(serverPlayer.level())
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 6)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 12)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 18)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 1, 0);
                    if (blizzard)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_tomorrow"), false);
                    else if (snow)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.snow_tomorrow"), false);
                    else
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.clear_tomorrow"), false);
                }
            }

            if (serverPlayer.level().getDayTime() % 24000 == 41 && FHConfig.COMMON.enableDailyKitchen.get())
                DailyKitchen.generateWantedFood(serverPlayer);//This is daily kitchen thing,not forecast message.
        }
    }

    /*
     * Movement modifiers for snowshoes and ice skates
     */
    @SubscribeEvent
    public static void movementModifier(TickEvent.PlayerTickEvent event) {
        Level world = event.player.level();
        Player player = event.player;
        BlockPos pos;
        if (player.getY() % 1 < 0.5) {
            pos = player.blockPosition().below();
        } else {
            pos = player.blockPosition();
        }
        Block ground = world.getBlockState(pos).getBlock();

        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        Item feetEquipment = player.getItemBySlot(EquipmentSlot.FEET).getItem();
        Item headEquipment = player.getItemBySlot(EquipmentSlot.HEAD).getItem();

        if (movementSpeed == null || swimSpeed == null) {
            return;
        }

        // check if match FHTags.Blocks.SNOW_MOVEMENT
        boolean isSnowBlock = world.getBlockState(player.blockPosition()).getTags().anyMatch(t -> t == FHTags.Blocks.SNOW_MOVEMENT);
        boolean isSnowBlockBelow = world.getBlockState(player.blockPosition().below()).getTags().anyMatch(t -> t == FHTags.Blocks.SNOW_MOVEMENT);
        boolean isIceBlock = world.getBlockState(player.blockPosition()).getTags().anyMatch(t -> t == FHTags.Blocks.ICE_MOVEMENT);
        boolean isIceBlockBelow = world.getBlockState(player.blockPosition().below()).getTags().anyMatch(t -> t == FHTags.Blocks.ICE_MOVEMENT);

        if (feetEquipment == FHItems.SNOWSHOES.get()) {
            if ((isSnowBlock || isSnowBlockBelow) && !movementSpeed.hasModifier(FHAttributes.SNOW_DRIFTER)) {
                movementSpeed.addTransientModifier(FHAttributes.SNOW_DRIFTER);
                player.setMaxUpStep(1.0f);
            }
        } else if (feetEquipment != FHItems.SNOWSHOES.get() && movementSpeed.hasModifier(FHAttributes.SNOW_DRIFTER)) {
            movementSpeed.removeModifier(FHAttributes.SNOW_DRIFTER);
            player.setMaxUpStep(0.5f);
        }
        if ((!isSnowBlock && !isSnowBlockBelow) && ground != Blocks.AIR && movementSpeed.hasModifier(FHAttributes.SNOW_DRIFTER)) {
            movementSpeed.removeModifier(FHAttributes.SNOW_DRIFTER);
            player.setMaxUpStep(0.5f);
        }
        if (feetEquipment == FHItems.ICE_SKATES.get()) {
            if ((isIceBlock || isIceBlockBelow) && !movementSpeed.hasModifier(FHAttributes.SPEED_SKATER)) {
                movementSpeed.addTransientModifier(FHAttributes.SPEED_SKATER);
                player.setMaxUpStep(1.0f);
            }
        } else if (feetEquipment != FHItems.ICE_SKATES.get() && movementSpeed.hasModifier(FHAttributes.SPEED_SKATER)) {
            movementSpeed.removeModifier(FHAttributes.SPEED_SKATER);
            player.setMaxUpStep(0.5f);
        }
        if ((!isIceBlock && !isIceBlockBelow) && ground != Blocks.AIR && movementSpeed.hasModifier(FHAttributes.SPEED_SKATER)) {
            movementSpeed.removeModifier(FHAttributes.SPEED_SKATER);
            player.setMaxUpStep(0.5f);
        }
    }
}
