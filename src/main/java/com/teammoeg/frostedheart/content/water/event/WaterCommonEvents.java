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

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.water.network.PlayerDrinkWaterMessage;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.content.water.util.WaterLevelUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;


@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaterCommonEvents {
    static int tick = 0;

    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        //Common capabilities
        event.addCapability(FHMain.rl("water_level"), FHCapabilities.PLAYER_WATER_LEVEL.provider());
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
        if (FHConfig.SERVER.resetWaterLevelInDeath.get()) {
            flag = flag && !event.isWasDeath();
        }
        if (flag && WaterLevelCapability.getCapability(player).isPresent()) {
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

    @SubscribeEvent
    public static void EntityJoinWorldEvent(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && !(entity instanceof FakePlayer)) {
            WaterLevelCapability.getCapability(serverPlayer).ifPresent(t -> FHNetwork.INSTANCE.sendPlayer(serverPlayer, new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
        }
    }


    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        tick++;
        tick %= 8000;
        Player player = event.player;
        Level level = player.level();
        if (!(player instanceof ServerPlayer)) return;
        if(player.isCreative() || player.isSpectator()) return;
        if (WaterLevelUtil.canPlayerAddWaterExhaustionLevel(player)) {
            if (tick % 2 == 0) {
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
/*
                Biome biome = level.getBiome(BlockPos.containing(player.getPosition(0f).x,player.getPosition(0f).y,player.getPosition(0f).z)).value();
                if (level.getLightEmission(BlockPos.containing(player.getPosition(0f).x,player.getPosition(0f).y,player.getPosition(0f).z)) == 15 && level.getDayTime() < 11000 && level.getDayTime() > 450 && !level.isRainingAt(BlockPos.containing(player.getPosition(0f).x,player.getPosition(0f).y,player.getPosition(0f).z))) {
                    if (biome.getBaseTemperature() > 0.3) {
                        WaterLevelCapability.getCapability(player).ifPresent(data -> {
                            data.addExhaustion(player, 0.0075f);
                        });
                    }
                    if (biome.getBaseTemperature() > 0.9) {
                        WaterLevelCapability.getCapability(player).ifPresent(data -> {
                            data.addExhaustion(player, 0.0055f);
                        });
                    }
                }*/
                //}
                //Thirty State
                MobEffectInstance effectInstance = player.getEffect(FHMobEffects.THIRST.get());
                if (effectInstance != null) {
                    WaterLevelCapability.getCapability(player).ifPresent(data -> {
                        data.addExhaustion(player, 0.07f + 0.05f * effectInstance.getAmplifier());
                    });
                }
            }
        }
        //Punishment/Reward - 5s
        if (tick % 250 == 0 && !(player instanceof FakePlayer)) {
            WaterLevelCapability.getCapability(player).ifPresent(data -> {
                if (!player.isCreative()) {
                    data.punishment(player);
                    data.award(player);
                }
            });
        }
        //Restore water level in Peaceful difficulty mode - 3s
        if (tick % 150 == 0 && !(player instanceof FakePlayer)) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                WaterLevelCapability.getCapability(player).ifPresent(data -> {
                    data.restoreWater(player, 2);
                });
            }
        }
        //Update water between server and client - 30s
        if (tick % 1500 == 0 && !(player instanceof FakePlayer) && !level.isClientSide()) {
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

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        ItemStack heldItem = event.getItemStack();
        Player player = event.getEntity();
        //drink water block
        if (heldItem.isEmpty() && event.getLevel().getFluidState(event.getHitVec().getBlockPos().offset(event.getFace().getNormal())).getType() == Fluids.WATER && player.getPose() == Pose.CROUCHING) {
            drinkWaterBlock(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        //drink water block
        HitResult hitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (player.getPose() == Pose.CROUCHING && hitresult.getType() == HitResult.Type.BLOCK && level.getFluidState(BlockPos.containing(hitresult.getLocation().x,hitresult.getLocation().y,hitresult.getLocation().z)).getType() == Fluids.WATER) {
            level.playSound(player,BlockPos.containing(player.getPosition(0f).x,player.getPosition(0f).y,player.getPosition(0f).z), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.4f, 1.0f);
            FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage());
        }
    }

    public static void drinkWaterBlock(Player player) {
        Level world = player.level();
        WaterLevelCapability.getCapability(player).ifPresent(data -> {
            data.addWaterLevel(player, 1);
            world.playSound(player, BlockPos.containing(player.getPosition(0f).x,player.getPosition(0f).y,player.getPosition(0f).z), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.4f, 1.0f);
            //0.05 poising，0.8 thirsty；
            if (!world.isClientSide()) {
                Random random = new Random();
                double d1 = random.nextDouble();
                double d2 = random.nextDouble();
                if (d1 <= 0.05D) player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
                if (d2 <= 0.8D) player.addEffect(new MobEffectInstance(FHMobEffects.THIRST.get(), 900, 0));
            }
        });
    }

    //from Item.class=
    protected static BlockHitResult getPlayerPOVHitResult(Level pLevel, Player pPlayer, ClipContext.Fluid pFluidMode) {
        float f = pPlayer.getXRot();
        float f1 = pPlayer.getYRot();
        Vec3 vec3 = pPlayer.getEyePosition();
        float f2 = Mth.cos(-f1 * 0.017453292F - 3.1415927F);
        float f3 = Mth.sin(-f1 * 0.017453292F - 3.1415927F);
        float f4 = -Mth.cos(-f * 0.017453292F);
        float f5 = Mth.sin(-f * 0.017453292F);
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = pPlayer.getBlockReach();
        Vec3 vec31 = vec3.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        return pLevel.clip(new ClipContext(vec3, vec31, net.minecraft.world.level.ClipContext.Block.OUTLINE, pFluidMode, pPlayer));
    }
}
