/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.network.FHDataSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.util.FHEffects;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class TemperatureUpdate {
	public static final float HEAT_EXCHANGE_CONSTANT=0.0012F;
    /**
     * Perform temperature tick logic
     * @param event fired every tick on player
     */
    @SubscribeEvent
    public static void updateTemperature(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
                && event.getEntityLiving() instanceof ServerPlayerEntity) {

            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            if (player.ticksExisted % 10 != 0 || player.isCreative() || player.isSpectator())
                return;
            if(player.isInWaterOrBubbleColumn()) {
            	player.addPotionEffect(new EffectInstance(FHEffects.WET, 400, 0));
            }
            float current = TemperatureCore.getBodyTemperature(player);
            if (current < 0)
                current += 0.05;
            World world = player.getEntityWorld();
            BlockPos pos = player.getPosition();
            float envtemp = ChunkData.getTemperature(world, pos);
            float skyLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.SKY).getLightFor(pos);
            float gameTime = world.getDayTime() % 24000L;
            gameTime = gameTime / (200 / 3);
            gameTime = (float) Math.sin(Math.toRadians(gameTime));
            envtemp += TemperatureCore.getBlockTemp(world, pos);
            envtemp += skyLight > 5.0F ? (gameTime * 5.0F) : (-1.0F * 5.0F);
            envtemp -= 37F;// normalize
            float keepwarm = 0;
            if (player.isBurning())
                envtemp += 150F;
            for (ItemStack is : CuriosCompat.getAllCuriosIfVisible(player)) {
                if (is == null)
                    continue;
                Item it = is.getItem();
                if (it instanceof IHeatingEquipment)
                    current = ((IHeatingEquipment) it).compute(is, current, envtemp);
                if (it instanceof IWarmKeepingEquipment) {
                    keepwarm += ((IWarmKeepingEquipment) it).getFactor(player, is);
                } else {
                    IWarmKeepingEquipment iw = FHDataManager.getArmor(is);
                    if (iw != null)
                        keepwarm += iw.getFactor(player, is);
                }
            }
            for (ItemStack is : player.getArmorInventoryList()) {
                if (is == null)
                    continue;
                Item it = is.getItem();
                if (it instanceof IHeatingEquipment)
                    current = ((IHeatingEquipment) it).compute(is, current, envtemp);
                if (it instanceof IWarmKeepingEquipment) {
                    keepwarm += ((IWarmKeepingEquipment) it).getFactor(player, is);
                } else {
                    IWarmKeepingEquipment iw = FHDataManager.getArmor(is);
                    if (iw != null)
                        keepwarm += iw.getFactor(player, is);
                }
            }
            if (keepwarm > 1)
                keepwarm = 1;
            current +=  HEAT_EXCHANGE_CONSTANT* (1 - keepwarm) * (envtemp - current);
            if (current < -10)
                current = -10;
            else if (current > 10)
                current = 10;

            TemperatureCore.setTemperature(player, current, envtemp + 37);
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new FHDataSyncPacket(player));
        }
    }

    /**
     * Perform temperature effect
     * @param event fired every tick on player
     */
    @SubscribeEvent
    public static void regulateTemperature(LivingUpdateEvent event) {
        if (event.getEntityLiving() != null && !(event.getEntityLiving()).world.isRemote
                && event.getEntityLiving() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            double calculatedTarget = TemperatureCore.getBodyTemperature(player);
            if (!(player.isCreative() || player.isSpectator())) {
                if (calculatedTarget > 1 || calculatedTarget < -1) {
                    if (!player.isPotionActive(FHEffects.HYPERTHERMIA) && !player.isPotionActive(FHEffects.HYPOTHERMIA)) {
                        if (calculatedTarget > 1) { // too hot
                            if (calculatedTarget <= 2) {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, 0));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            } else if (calculatedTarget <= 3) {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, 1));
                                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            } else if (calculatedTarget <= 5) {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            } else {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, (int) (calculatedTarget-2)));
                                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            }
                        } else { // too cold
                            if (calculatedTarget >= -2) {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, 0));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            } else if (calculatedTarget >= -3) {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, 1));
                                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            } else if (calculatedTarget >= -5) {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            } else {
                            	player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, (int) (-calculatedTarget-2)));
                                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
                                player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
                            }
                        }
                    }
                }
            }
        }
    }
}
