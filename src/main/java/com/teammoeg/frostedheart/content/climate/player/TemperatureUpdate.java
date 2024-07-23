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

package com.teammoeg.frostedheart.content.climate.player;

import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.constants.EquipmentCuriosSlotType;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class TemperatureUpdate {
    public static final float HEAT_EXCHANGE_CONSTANT = 0.0012F;

    public static final float SELF_HEATING_CONSTANT = 0.036F;

    
    /**
     * Perform temperature effect
     *
     * @param event fired every tick on player
     */
    @SubscribeEvent
    public static void regulateTemperature(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.END
                && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            double calculatedTarget = PlayerTemperatureData.getCapability(event.player).map(PlayerTemperatureData::getBodyTemp).orElse(0f);
            if (!(player.isCreative() || player.isSpectator())) {
                if (calculatedTarget > 1 || calculatedTarget < -1) {
                    if (!player.hasEffect(FHEffects.HYPERTHERMIA.get())
                            && !player.hasEffect(FHEffects.HYPOTHERMIA.get())) {
                        if (calculatedTarget > 1) { // too hot
                            if (calculatedTarget <= 2) {
                                player.addEffect(new MobEffectInstance(FHEffects.HYPERTHERMIA.get(), 100, 0));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget <= 3) {
                                player.addEffect(new MobEffectInstance(FHEffects.HYPERTHERMIA.get(), 100, 1));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget <= 5) {
                                player.addEffect(new MobEffectInstance(FHEffects.HYPERTHERMIA.get(), 100, 2));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHEffects.HYPERTHERMIA.get(), 100, (int) (calculatedTarget - 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            }
                        } else { // too cold
                            if (calculatedTarget >= -2) {
                                player.addEffect(new MobEffectInstance(FHEffects.HYPOTHERMIA.get(), 100, 0));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget >= -3) {
                                player.addEffect(new MobEffectInstance(FHEffects.HYPOTHERMIA.get(), 100, 1));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget >= -5) {
                                player.addEffect(new MobEffectInstance(FHEffects.HYPOTHERMIA.get(), 100, 2));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHEffects.HYPOTHERMIA.get(), 100, (int) (-calculatedTarget - 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            }
                        }
                    }
                }
            }
        }
    }
    public static final UUID envTempId=UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");
    /**
     * Perform temperature tick logic
     * <p>
     * Updated every 10 ticks (0.5s)
     *
     * @param event fired every tick on player
     */
    @SubscribeEvent
    public static void updateTemperature(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            // ignore creative and spectator players
            // no longer ignore for easier debug
           /* if (player.isCreative() || player.isSpectator())
                return;*/
            PlayerTemperatureData data= PlayerTemperatureData.getCapability(event.player).orElse(null);
            if(data==null)return;
            if (player.tickCount % 10 == 0) {
                //soak in water modifier
                if (player.isInWater()) {
                    boolean hasArmor = false;
                    for (ItemStack is : player.getArmorSlots()) {
                        if (!is.isEmpty()) {
                            hasArmor = true;
                            break;
                        }
                    }
                    MobEffectInstance current = player.getEffect(FHEffects.WET.get());
                    if (hasArmor)
                        player.addEffect(new MobEffectInstance(FHEffects.WET.get(), 400, 0));// punish for wet clothes
                    else if (current == null || current.getDuration() < 100)
                        player.addEffect(new MobEffectInstance(FHEffects.WET.get(), 100, 0));
                }
                //load current data
                float current = PlayerTemperatureData.getCapability(event.player).map(PlayerTemperatureData::getBodyTemp).orElse(0f);
                double tspeed = FHConfig.SERVER.tempSpeed.get();
                if (current < 0) {
                    float delt = (float) (FHConfig.SERVER.tdiffculty.get().self_heat.apply(player) * tspeed);
                    player.causeFoodExhaustion(Math.min(delt, -current) * 0.5f);//cost hunger for cold.
                    current += delt;
                }
                //world and chunk temperature
                Level world = player.getCommandSenderWorld();
                BlockPos pos = new BlockPos(player.getX(), player.getEyeY(), player.getZ());

                //Temperature from generators
                float envtemp = ChunkHeatData.getAdditionTemperature(world, pos);
                //Temperature from world basis and biome basis
                envtemp += WorldTemperature.getBaseTemperature(world, pos);
                //Temperature from climate
                envtemp += WorldTemperature.getClimateTemperature(world);
                //Surrounding temperature
                Pair<Float, Float> btp = new SurroundingTemperatureSimulator(player).getBlockTemperatureAndWind(player.getX(), player.getEyeY()-0.7f, player.getZ());
                float bt=btp.getFirst();
                //int wind=btp.getSecond()+WorldTemperature.getClimateWind(world);
                //Day-night temperature
                float skyLight = world.getChunkSource().getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
                float gameTime = world.getDayTime() % 24000L;
                gameTime = gameTime / ((float) 200 / 3);
                gameTime = Mth.sin((float) Math.toRadians(gameTime));
                envtemp += bt;
                envtemp += skyLight > 5.0F ?
                        (world.isRaining() ?
                                (FHUtils.isRainingAt(player.blockPosition(), world) ? -8F : -5f)
                                : (gameTime * 5.0F))
                        : -5F;
                // burning heat
                if (player.isOnFire())
                    envtemp += 150F;
                player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).removeModifier(envTempId);
                player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).addTransientModifier(new AttributeModifier(envTempId,"player environment modifier", envtemp, Operation.ADDITION));
                
                
                envtemp=(float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
                float insulation = (float) player.getAttributeValue(FHAttributes.INSULATION.get());
                float efftemp=current-(1-insulation)*(current-envtemp);//Effective temperature
                
                
                
                
                //list of equipments to be calculated
                for (Pair<String, ItemStack> is : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                    if (is == null)
                        continue;
                    Item it = is.getSecond().getItem();
                    if (it instanceof IHeatingEquipment)
                    	efftemp+=((IHeatingEquipment) it).getEffectiveTempAdded(EquipmentCuriosSlotType.fromCurios(is.getFirst()), is.getSecond(), efftemp, current);
                }
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                	ItemStack is=player.getItemBySlot(slot);
                    if (is.isEmpty())
                        continue;
                    Item it = is.getItem();
                    if (it instanceof IHeatingEquipment) {
                    	if (it instanceof IHeatingEquipment)
                        	efftemp+=((IHeatingEquipment) it).getEffectiveTempAdded(EquipmentCuriosSlotType.fromVanilla(slot), is, efftemp, current);
                    }
                    /*if (it instanceof IWarmKeepingEquipment) {
                        keepwarm += ((IWarmKeepingEquipment) it).getFactor(player, is);
                    } else {//include inner
                        String s = ItemNBTHelper.getString(is, "inner_cover");
                        IWarmKeepingEquipment iw = null;
                        EquipmentSlotType aes = MobEntity.getSlotForItemStack(is);
                        if (s.length() > 0 && aes != null) {
                            iw = FHDataManager.getArmor(s + "_" + aes.getName());
                        } else
                            iw = FHDataManager.getArmor(is);
                        if (iw != null)
                            keepwarm += iw.getFactor(player, is);
                    }*/
                }
                //environment heat exchange
                float dheat = HEAT_EXCHANGE_CONSTANT * ((efftemp-37F) - current);
                //Attack player if temperature changes too much
                if (dheat > 0.1)
                    player.hurt(FHDamageSources.HYPERTHERMIA_INSTANT, (dheat) * 10);
                else if (dheat < -0.1)
                    player.hurt(FHDamageSources.HYPOTHERMIA_INSTANT, (-dheat) * 10);
                if (!player.isCreative()&&!player.isSpectator())//no modify body temp when creative or spectator
                	current += (float) (dheat * tspeed);
                if (current < -10)
                    current = -10;
                else if (current > 10)
                    current = 10;
                float lenvtemp = data.getEnvTemp();//get a smooth change in display
                float lfeeltemp=data.getFeelTemp();
                
                data.update(current, (envtemp) * .2f + lenvtemp * .8f, (efftemp)*.2f+lfeeltemp*.8f);
                //FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHBodyDataSyncPacket(player));
            }

            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHBodyDataSyncPacket(player));
        }
    }
}
