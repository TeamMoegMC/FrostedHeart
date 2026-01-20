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

package com.teammoeg.frostedheart.content.water.event;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.content.water.util.WaterLevelUtil;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaterCommonEvents {

    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        //Common capabilities
        if (event.getObject() instanceof Player player) {
            if (!(player instanceof FakePlayer)) {
                event.addCapability(FHMain.rl("water_level"), FHCapabilities.PLAYER_WATER_LEVEL.provider());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingEntityUseItemEventFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        ItemStack stack = event.getItem();
        if (entity instanceof ServerPlayer player) {
            WaterLevelUtil.drink(player, stack);
            //FHNetwork.sendPlayer(player, new PlayerDrinkWaterMessage());
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            if (WaterLevelUtil.canPlayerAddWaterExhaustionLevel(player)) {
                WaterLevelCapability.getCapability(player).ifPresent(data -> {
                    if (entity.isSprinting()) {
                        data.addExhaustion(player, 0.12f);
                    } else data.addExhaustion(player, 0.07f);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerEventClone(PlayerEvent.Clone event) {
        boolean flag;
        Player player = event.getEntity();
        flag = !(player instanceof FakePlayer) && player instanceof ServerPlayer;
        if (FHConfig.SERVER.NUTRITION.resetWaterLevelInDeath.get()) {
            flag = flag && !event.isWasDeath();
        }
        if (flag) {
        	event.getOriginal().reviveCaps();
        	WaterLevelCapability.getCapability(player).ifPresent(date -> {
                WaterLevelCapability.getCapability(event.getOriginal()).ifPresent(t -> {
                    date.setWaterLevel(t.getWaterLevel());
                    date.setWaterExhaustionLevel(t.getWaterExhaustionLevel());
                    date.setWaterSaturationLevel(t.getWaterSaturationLevel());
                });
            });
            WaterLevelCapability.getCapability(player).ifPresent(t -> FHNetwork.INSTANCE.sendPlayer((ServerPlayer) player, new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            WaterLevelCapability.getCapability(serverPlayer).ifPresent(t -> FHNetwork.INSTANCE.sendPlayer(serverPlayer, new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
            //CriteriaTriggerRegistry.GUIDE_BOOK_TRIGGER.trigger(player);
        }
    }
/*
    @SubscribeEvent
    public static void EntityJoinWorldEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && !(entity instanceof FakePlayer)) {
            WaterLevelCapability.getCapability(serverPlayer).ifPresent(t -> FHNetwork.INSTANCE.sendPlayer(serverPlayer, new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
        }
    }*/


    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();
        if (!(player instanceof ServerPlayer)) return;
        if(player.isCreative() || player.isSpectator()) return;
        long tick = level.getGameTime();
        if (WaterLevelUtil.canPlayerAddWaterExhaustionLevel(player)) {
            if (tick % 40 == 0) {
                player.getDeltaMovement();
                if (player.onGround() || player.isInWater()) {
                    double x = player.getDeltaMovement().length();
                    if (x < 5) {
                        WaterLevelCapability.getCapability(player).ifPresent(dataW -> {
                            if (player.isSprinting()) {
                                dataW.addExhaustion(player, (float) (x / 15));
                            } else dataW.addExhaustion(player, (float) (x / 30));
                        });
                    }
                }
            }

            if (tick % 10 == 0) {
                //WaterRestoring effect
                MobEffectInstance effectInstance1 = player.getEffect(FHMobEffects.WATER_RESTORING.get());
                if (effectInstance1 != null) {
                    WaterLevelCapability.getCapability(player).ifPresent(data -> {
                        data.restoreWater(player, 1);
                    });
                }

                //Thirty State
                // TODO: no need for this. we already punish for thirst in addExhaust
//                MobEffectInstance effectInstance = player.getEffect(FHMobEffects.THIRST.get());
//                if (effectInstance != null) {
//                    WaterLevelCapability.getCapability(player).ifPresent(data -> {
//                        data.addExhaustion(player, 0.07f + 0.05f * effectInstance.getAmplifier());
//                    });
//                }
            }
        }
        //Punishment/Reward - 30s
        if (tick % 600 == 0 && !(player instanceof FakePlayer)) {
            WaterLevelCapability.getCapability(player).ifPresent(data -> {
                if (!player.isCreative()) {
                    data.punishment(player);
                    // TODO: don't understand what this does.
                    data.award(player);
                }
            });
        }
        //Restore water level in Peaceful difficulty mode - 3s
        if (tick % 60 == 0 && !(player instanceof FakePlayer)) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                WaterLevelCapability.getCapability(player).ifPresent(data -> {
                    data.restoreWater(player, 2);
                });
            }
        }
        //Update water between server and client - 30s
        if (tick % 600 == 0 && !(player instanceof FakePlayer) && !level.isClientSide()) {
            WaterLevelCapability.getCapability(player).ifPresent(data -> {
                FHNetwork.INSTANCE.sendPlayer((ServerPlayer) player, new PlayerWaterLevelSyncPacket(data.getWaterLevel(), data.getWaterSaturationLevel(), data.getWaterExhaustionLevel()));
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (WaterLevelUtil.canPlayerAddWaterExhaustionLevel(player) && !player.level().isClientSide()) {
            WaterLevelCapability.getCapability(player).ifPresent(data -> data.addExhaustion(player, 0.005f));
        }
    }

}
