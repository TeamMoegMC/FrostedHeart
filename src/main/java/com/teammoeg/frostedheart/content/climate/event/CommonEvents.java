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

package com.teammoeg.frostedheart.content.climate.event;

import com.mojang.brigadier.CommandDispatcher;
import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.FHDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.FHDataManager.DataType;
import com.teammoeg.frostedheart.base.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.command.*;
import com.teammoeg.frostedheart.content.agriculture.FHBerryBushBlock;
import com.teammoeg.frostedheart.content.agriculture.FHCropBlock;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.network.FHDatapackSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.foods.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.constants.EquipmentCuriosSlotType;

import blusunrize.immersiveengineering.common.register.IEBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {
    @SubscribeEvent
    public static void insulationDataAttr(ItemAttributeModifierEvent event) {
        ArmorTempData data=FHDataManager.getArmor(event.getItemStack());

        if(data!=null) {
        	EquipmentSlot es=event.getItemStack().getEquipmentSlot();
        	if(es!=null) {
	        	EquipmentCuriosSlotType ecs=EquipmentCuriosSlotType.fromVanilla(es);
	        	if(event.getSlotType()==es) {
		        	if(data.getInsulation()!=0)
		        		event.addModifier(FHAttributes.INSULATION.get(), new AttributeModifier(ecs.getSlotUUID(0), ecs.getKey(0), data.getInsulation(), Operation.ADDITION));
		        	if(data.getColdProof()!=0)
		        		event.addModifier(FHAttributes.WIND_PROOF.get(), new AttributeModifier(ecs.getSlotUUID(0), ecs.getKey(0), data.getColdProof(), Operation.ADDITION));
		        	if(data.getHeatProof()!=0)
		        		event.addModifier(FHAttributes.HEAT_PROOF.get(), new AttributeModifier(ecs.getSlotUUID(0), ecs.getKey(0), data.getHeatProof(), Operation.ADDITION));
	        	}
        	}
        }
    }


    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(FHDataManager.INSTANCE);
    }

    @SubscribeEvent
    public static void beforeCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        Block growBlock = event.getState().getBlock();
        BlockPos belowPos=event.getPos().below();
        Block belowGrowBlock=event.getLevel().getBlockState(belowPos).getBlock();
        float temp = ChunkHeatData.getTemperature(event.getLevel(), event.getPos());
        boolean bz = WorldClimate.isBlizzard(event.getLevel());
        if (bz) {
        	BlockPos cur=event.getPos();

        	/*if(!(growBlock instanceof IGrowable)) {
	        	if(belowGrowBlock instanceof IGrowable)
	        		cur=belowPos;
	        	else {
	        		event.setResult(Event.Result.DENY);
	        		return;
	        	}

        	}*/
            if (FHUtils.isBlizzardHarming(event.getLevel(), event.getPos())) {
            	FluidState curstate=event.getLevel().getFluidState(cur);
            	if(curstate.isEmpty())
            		event.getLevel().setBlock(cur, Blocks.AIR.defaultBlockState(), 2);
            	else
            		event.getLevel().setBlock(cur, curstate.createLegacyBlock(), 2);
            } else if (event.getLevel().getRandom().nextInt(3) == 0) {
            	//FluidState curstate=event.getLevel().getFluidState(cur);

                event.getLevel().setBlock(cur,growBlock.defaultBlockState(), 2);
            }
            event.setResult(Event.Result.DENY);
        } else if (growBlock instanceof FHCropBlock) {
        } else if (growBlock==(IEBlocks.Misc.HEMP_PLANT.get())) {
            if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                if (event.getLevel().getRandom().nextInt(3) == 0) {
                    event.getLevel().setBlock(event.getPos(), growBlock.defaultBlockState(), 2);
                }
                event.setResult(Event.Result.DENY);
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (event.getLevel().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getLevel().getBlockState(event.getPos());
                    if (cbs.is(growBlock))
                        event.getLevel().setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }
        } else if(growBlock instanceof BonemealableBlock){
            if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE) {
                // Set back to default state, might not be necessary
                if (event.getLevel().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getLevel().getBlockState(event.getPos());
                    if (cbs.is(growBlock) && cbs != growBlock.defaultBlockState())
                        event.getLevel().setBlock(event.getPos(), growBlock.defaultBlockState(), 2);
                }
                event.setResult(Event.Result.DENY);
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (event.getLevel().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getLevel().getBlockState(event.getPos());
                    if (cbs.is(growBlock))
                        event.getLevel().setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }

        }else {
        	if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE)
        		event.setResult(Event.Result.DENY);
        }
    }
    @SubscribeEvent
    public static void finishedEatingFood(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() != null && !event.getEntity().level().isClientSide
                && event.getEntity() instanceof ServerPlayer) {
            ItemStack is = event.getItem();
            Item it = event.getItem().getItem();
            ITempAdjustFood adj = null;
            // System.out.println(it.getRegistryName());
            double tspeed = FHConfig.SERVER.tempSpeed.get();
            if (it instanceof ITempAdjustFood) {
                adj = (ITempAdjustFood) it;
            } else {
                adj = FHDataManager.getFood(is);
            }
            if (adj != null) {
                float current = PlayerTemperatureData.getCapability((ServerPlayer) event.getEntity()).map(PlayerTemperatureData::getBodyTemp).orElse(0f);
                float max = adj.getMaxTemp(event.getItem());
                float min = adj.getMinTemp(event.getItem());
                float heat = adj.getHeat(event.getItem(),PlayerTemperatureData.getCapability((ServerPlayer) event.getEntity()).map(PlayerTemperatureData::getEnvTemp).orElse(0f));
                if (heat > 1) {
                    event.getEntity().hurt(FHDamageSources.HYPERTHERMIA_INSTANT, (heat) * 2);
                } else if (heat < -1)
                    event.getEntity().hurt(FHDamageSources.HYPOTHERMIA_INSTANT, (heat) * 2);
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
                final float toset=current;
                PlayerTemperatureData.getCapability((ServerPlayer) event.getEntity()).ifPresent(t->t.setBodyTemp(toset));
            }

            DailyKitchen.tryGiveBenefits((ServerPlayer) event.getEntity(), is);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onArmorDamage(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player && (event.getSource().is(DamageTypeTags.IS_FIRE) || !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))) {
            Player player = (Player) event.getEntity();
            float damage = event.getAmount();
            DamageSource p_234563_1_ = event.getSource();
            if (damage > 0) {
                damage = damage / 8.0F;
                if (p_234563_1_.is(DamageTypeTags.IS_FIRE))// fire damage more
                    damage *= 2;
                else if (p_234563_1_.is(DamageTypeTags.IS_EXPLOSION))// explode add a lot
                    damage *= 4;
                int amount = (int) damage;
                if (amount != damage)
                    amount += player.getRandom().nextDouble() < (damage - amount) ? 1 : 0;
                if (amount <= 0)
                    return;
                for (ItemStack itemstack : player.getArmorSlots()) {
                    if (itemstack.isEmpty())
                        continue;
                    CompoundTag cn = itemstack.getTag();
                    if (cn == null)
                        continue;
                    String inner = cn.getString("inner_cover");
                    if (inner.isEmpty())
                        continue;
                    if (cn.getBoolean("inner_bounded")) {
                        int dmg = cn.getInt("inner_damage");
                        if (dmg < itemstack.getDamageValue()) {
                            dmg = itemstack.getDamageValue();
                        }
                        dmg += amount;
                        if (dmg >= itemstack.getMaxDamage()) {
                            cn.remove("inner_cover");
                            cn.remove("inner_cover_tag");
                            cn.remove("inner_bounded");
                            cn.remove("inner_damage");
                            player.broadcastBreakEvent(Mob.getEquipmentSlotForItem(itemstack));
                        } else cn.putInt("inner_damage", dmg);
                        continue;
                    }
                    CompoundTag cnbt = cn.getCompound("inner_cover_tag");
                    int i = FHUtils.getEnchantmentLevel(Enchantments.UNBREAKING, cnbt);
                    int j = 0;
                    if (i > 0)
                        for (int k = 0; k < amount; ++k) {
                            if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(itemstack, i, player.getRandom())) {
                                ++j;
                            }
                        }
                    amount -= j;
                    if (amount <= 0)
                        continue;
                    int crdmg = cnbt.getInt("Damage");
                    crdmg += amount;
                    InstallInnerRecipe ri = InstallInnerRecipe.recipeList.get(new ResourceLocation(inner));

                    if (ri != null && ri.getDurability() <= crdmg) {// damaged
                        cn.remove("inner_cover");
                        cn.remove("inner_cover_tag");
                        cn.remove("inner_bounded");
                        player.broadcastBreakEvent(Mob.getEquipmentSlotForItem(itemstack));
                    } else {
                        cnbt.putInt("Damage", crdmg);
                        cn.put("inner_cover_tag", cnbt);
                    }
                }

            }
        }
    }

    // TODO create grow temperature mappings for every plant in the modpack
    @SubscribeEvent
    public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            Block growBlock = event.getPlacedBlock().getBlock();
            float temp = ChunkHeatData.getTemperature(event.getLevel(), event.getPos());
            if (growBlock instanceof BonemealableBlock) {
                if (growBlock instanceof SaplingBlock) {
                    if (temp < -5) {
                    	player.displayClientMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(-6)), true);
                    }
                } else if (growBlock instanceof FHCropBlock) {
                    int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.displayClientMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(growTemp)), true);
                    }
                } else if (growBlock instanceof FHBerryBushBlock) {
                    int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.displayClientMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(growTemp)), true);
                    }
                } else if (growBlock==(IEBlocks.Misc.HEMP_PLANT.get())) {
                    if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                        event.setCanceled(true);
                        player.displayClientMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(WorldTemperature.HEMP_GROW_TEMPERATURE)), true);
                    }
                } else if (growBlock == Blocks.NETHERRACK) {

                } else if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE) {
                    event.setCanceled(true);
                    player.displayClientMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE)), true);

                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        AddTempCommand.register(dispatcher);
        ClimateCommand.register(dispatcher);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START) {
            Level world = event.level;
            if (!world.isClientSide && world instanceof ServerLevel) {
                ServerLevel serverWorld = (ServerLevel) world;

                // Scheduled checks (e.g. town structures)
                SchedulerQueue.tickAll(serverWorld);

                // Town logic tick
                int i = 0;
                for (TeamDataHolder trd : FHTeamDataManager.INSTANCE.getAllData()) {
                    if (serverWorld.dimension().equals(trd.getData(SpecialDataTypes.GENERATOR_DATA).dimension)) {
                        if (serverWorld.getGameTime() % 20 == i % 20) {//Split town calculations to multiple seconds
                            if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
                                trd.getData(SpecialDataTypes.TOWN_DATA).tick(serverWorld);
                            }
                        }
                        if(serverWorld.getGameTime() == i + 1000) {
                            if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
                                trd.getData(SpecialDataTypes.TOWN_DATA).tickMorning(serverWorld);//execute only once a day
                            }
                        }
                    }
                    i++;
                }

                // Update clock source every second, and check hour data if it needs an update
                if (serverWorld.getGameTime() % 20 == 0) {
                    WorldClimate data = WorldClimate.get(serverWorld);
                    if(data!=null) {
	                    data.updateClock(serverWorld);
	                    data.updateCache(serverWorld);
	                    data.trimTempEventStream();
                    }

                }
            }

        }
    }
    @SubscribeEvent
    public static void onUseBoneMeal(BonemealEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            Block growBlock = event.getBlock().getBlock();
            float temp = ChunkHeatData.getTemperature(event.getLevel(), event.getPos());
            if (growBlock instanceof FHCropBlock) {
                int growTemp = ((FHCropBlock) growBlock).getGrowTemperature() + WorldTemperature.BONEMEAL_TEMPERATURE;
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.displayClientMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(growTemp)), true);
                }
            } else if (growBlock instanceof FHBerryBushBlock) {
                int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature() + WorldTemperature.BONEMEAL_TEMPERATURE;
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.displayClientMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(growTemp)), true);
                }
            } else if (growBlock==(IEBlocks.Misc.HEMP_PLANT.get())) {
                if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE) {
                    event.setCanceled(true);
                    player.displayClientMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(WorldTemperature.HEMP_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE)), true);
                }
            } else if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE) {
                event.setCanceled(true);
                player.displayClientMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE)), true);
            }
        }
    }
    @SubscribeEvent
    public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();
            PacketTarget currentPlayer=PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity());
            FHResearch.sendSyncPacket(currentPlayer);
            for(DataType type:DataType.types)
            	FHNetwork.send(currentPlayer,new FHDatapackSyncPacket(type));
            FHNetwork.send(currentPlayer,new FHResearchDataSyncPacket(ResearchDataAPI.getData((ServerPlayer) event.getEntity())));
            FHCapabilities.CLIMATE_DATA.getCapability(serverWorld).ifPresent((cap) -> FHNetwork.send(currentPlayer,new FHClimatePacket(cap)));
            //System.out.println("=x-x=");
            //System.out.println(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS.getValue(new ResourceLocation(FHMain.MODID,"add_loot")));
        }
    }

    @SubscribeEvent
    public static void syncDataWhenDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();

            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
                    new FHClimatePacket(WorldClimate.get(serverWorld)));
        }
    }
}
