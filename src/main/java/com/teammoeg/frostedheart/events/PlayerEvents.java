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


import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.foods.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {
    @SuppressWarnings("resource")
    public static void onRC(PlayerInteractEvent.RightClickItem rci) {
        if (!rci.getWorld().isRemote
                && RegistryUtils.getRegistryName(rci.getItemStack().getItem()).getNamespace().equals("projecte")) {
            rci.setCancellationResult(ActionResultType.SUCCESS);
            rci.setCanceled(true);
            World world = rci.getWorld();
            PlayerEntity player = rci.getPlayer();
            BlockPos pos = rci.getPos();
            ServerWorld serverWorld = (ServerWorld) world;
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;

            serverPlayerEntity.addPotionEffect(
                    new EffectInstance(Effects.BLINDNESS, (int) (100 * (world.rand.nextDouble() + 0.5)), 3));
            serverPlayerEntity.addPotionEffect(
                    new EffectInstance(Effects.NAUSEA, (int) (1000 * (world.rand.nextDouble() + 0.5)), 5));

            serverPlayerEntity.connection.sendPacket(
                    new STitlePacket(STitlePacket.Type.TITLE, GuiUtils.translateMessage("too_cold_to_transmute")));
            serverPlayerEntity.connection.sendPacket(
                    new STitlePacket(STitlePacket.Type.SUBTITLE, GuiUtils.translateMessage("magical_backslash")));

            double posX = pos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * 4.5D;
            double posY = pos.getY() + world.rand.nextInt(3) - 1;
            double posZ = pos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * 4.5D;
            if (world.hasNoCollisions(EntityType.WITCH.getBoundingBoxWithSizeApplied(posX, posY, posZ))
                    && EntitySpawnPlacementRegistry.canSpawnEntity(EntityType.WITCH, serverWorld, SpawnReason.NATURAL,
                    new BlockPos(posX, posY, posZ), world.getRandom())) {
                FHUtils.spawnMob(serverWorld, new BlockPos(posX, posY, posZ), new CompoundNBT(),
                        new ResourceLocation("minecraft", "witch"));
            }
        }
    }


    @SubscribeEvent
    public static void sendForecastMessages(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.player;
            boolean configAllows = FHConfig.COMMON.enablesTemperatureForecast.get();
            if (configAllows && ResearchDataAPI.getVariants(serverPlayer).getDouble("has_forecast")>0) {
                // Blizzard warning
                //float thisHour = WorldClimate.getTemp(serverPlayer.world);
                boolean thisHourB = WorldClimate.isBlizzard(serverPlayer.world);
                //float nextHour = WorldClimate.getFutureTemp(serverPlayer.world, 1);
                boolean nextHourB = WorldClimate.isFutureBlizzard(serverPlayer.world, 1);
                if (!thisHourB) { // not in blizzard yet
                    if (nextHourB) {
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.blizzard_warning")
                                .mergeStyle(TextFormatting.DARK_RED).mergeStyle(TextFormatting.BOLD), true);
                        // serverPlayer.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
                        // GuiUtils.translateMessage("forecast.blizzard_warning")));
                    }
                } else { // in blizzard now
                    if (!nextHourB) {
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.blizzard_retreating")
                                .mergeStyle(TextFormatting.GREEN).mergeStyle(TextFormatting.BOLD), true);
                        // serverPlayer.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
                        // GuiUtils.translateMessage("forecast.blizzard_retreating")));
                    }
                }

                // Morning forecast wakeup time
                if (serverPlayer.world.getDayTime() % 24000 == 40) {
                    float morningTemp = Math.round(WorldClimate.getTemp(serverPlayer.world) * 10) / 10.0F;
                    float noonTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 0, 6) * 10) / 10.0F;
                    float nightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 0, 12) * 10) / 10.0F;
                    float midnightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 0, 18) * 10) / 10.0F;
                    float tomorrowMorningTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 1, 0) * 10) / 10.0F;
                    TemperatureDisplayHelper.sendTemperatureStatus(serverPlayer, "forecast.morning", false, morningTemp-10, noonTemp-10,
                            nightTemp-10, midnightTemp-10, tomorrowMorningTemp-10);
                    boolean snow = morningTemp < WorldTemperature.SNOW_TEMPERATURE
                            || noonTemp < WorldTemperature.SNOW_TEMPERATURE || nightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || midnightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowMorningTemp < WorldTemperature.SNOW_TEMPERATURE;
                    boolean blizzard = WorldClimate.isBlizzard(serverPlayer.world)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 0, 6)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 0, 12)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 0, 18)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 1, 0);
                    if (blizzard)
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.blizzard_today"), false);
                    else if (snow)
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.snow_today"), false);
                    else
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.clear_today"), false);

                }

                // Night forecast bedtime
                if (serverPlayer.world.getDayTime() % 24000 == 12542) {
                    float nightTemp = Math.round(WorldClimate.getTemp(serverPlayer.world) * 10) / 10.0F;
                    float midnightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 0, 6) * 10) / 10.0F;
                    float tomorrowMorningTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 0, 12) * 10)
                            / 10.0F;
                    float tomorrowNoonTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 0, 18) * 10)
                            / 10.0F;
                    float tomorrowNightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.world, 1, 0) * 10)
                            / 10.0F;
                    TemperatureDisplayHelper.sendTemperatureStatus(serverPlayer, "forecast.night", false, nightTemp-10, midnightTemp-10,
                            tomorrowMorningTemp-10, tomorrowNoonTemp-10, tomorrowNightTemp-10);
                    boolean snow = nightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || midnightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowMorningTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowNoonTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowNightTemp < WorldTemperature.SNOW_TEMPERATURE;
                    boolean blizzard = WorldClimate.isBlizzard(serverPlayer.world)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 0, 6)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 0, 12)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 0, 18)
                            || WorldClimate.isFutureBlizzard(serverPlayer.world, 1, 0);
                    if (blizzard)
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.blizzard_tomorrow"), false);
                    else if (snow)
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.snow_tomorrow"), false);
                    else
                        serverPlayer.sendStatusMessage(GuiUtils.translateMessage("forecast.clear_tomorrow"), false);
                }
            }

            if (serverPlayer.world.getDayTime() % 24000 == 41 && FHConfig.COMMON.enableDailyKitchen.get())
                DailyKitchen.generateWantedFood(serverPlayer);//This is daily kitchen thing,not forecast message.
        }
    }
}
