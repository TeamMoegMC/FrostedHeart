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
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncAllPacket;
import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.constants.EquipmentCuriosSlotType;

import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.UnbreakingEnchantment;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {
    @SubscribeEvent
    public static void insulationDataAttr(ItemAttributeModifierEvent event) {
        ArmorTempData data=FHDataManager.getArmor(event.getItemStack());

        if(data!=null) {
        	EquipmentSlotType es=event.getItemStack().getEquipmentSlot();
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
        BlockPos belowPos=event.getPos().down();
        Block belowGrowBlock=event.getWorld().getBlockState(belowPos).getBlock();
        float temp = ChunkHeatData.getTemperature(event.getWorld(), event.getPos());
        boolean bz = WorldClimate.isBlizzard(event.getWorld());
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
            if (FHUtils.isBlizzardHarming(event.getWorld(), event.getPos())) {
            	FluidState curstate=event.getWorld().getFluidState(cur);
            	if(curstate.isEmpty())
            		event.getWorld().setBlockState(cur, Blocks.AIR.getDefaultState(), 2);
            	else
            		event.getWorld().setBlockState(cur, curstate.getBlockState(), 2);
            } else if (event.getWorld().getRandom().nextInt(3) == 0) {
            	//FluidState curstate=event.getWorld().getFluidState(cur);

                event.getWorld().setBlockState(cur,growBlock.getDefaultState(), 2);
            }
            event.setResult(Event.Result.DENY);
        } else if (growBlock instanceof FHCropBlock) {
        } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
            if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getWorld().getBlockState(event.getPos());
                    if (cbs.matchesBlock(growBlock))
                        event.getWorld().setBlockState(event.getPos(), Blocks.AIR.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }
        } else if(growBlock instanceof IGrowable){
            if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE) {
                // Set back to default state, might not be necessary
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getWorld().getBlockState(event.getPos());
                    if (cbs.matchesBlock(growBlock) && cbs != growBlock.getDefaultState())
                        event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getWorld().getBlockState(event.getPos());
                    if (cbs.matchesBlock(growBlock))
                        event.getWorld().setBlockState(event.getPos(), Blocks.AIR.getDefaultState(), 2);
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
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
                && event.getEntityLiving() instanceof ServerPlayerEntity) {
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
                float current = PlayerTemperatureData.getCapability((ServerPlayerEntity) event.getEntityLiving()).map(PlayerTemperatureData::getBodyTemp).orElse(0f);
                float max = adj.getMaxTemp(event.getItem());
                float min = adj.getMinTemp(event.getItem());
                float heat = adj.getHeat(event.getItem(),PlayerTemperatureData.getCapability((ServerPlayerEntity) event.getEntityLiving()).map(PlayerTemperatureData::getEnvTemp).orElse(0f));
                if (heat > 1) {
                    event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPERTHERMIA_INSTANT, (heat) * 2);
                } else if (heat < -1)
                    event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPOTHERMIA_INSTANT, (heat) * 2);
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
                PlayerTemperatureData.getCapability((ServerPlayerEntity) event.getEntityLiving()).ifPresent(t->t.setBodyTemp(toset));
            }

            DailyKitchen.tryGiveBenefits((ServerPlayerEntity) event.getEntityLiving(), is);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onArmorDamage(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity && (event.getSource().isFireDamage() || !event.getSource().isUnblockable())) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            float damage = event.getAmount();
            DamageSource p_234563_1_ = event.getSource();
            if (damage > 0) {
                damage = damage / 8.0F;
                if (p_234563_1_.isFireDamage())// fire damage more
                    damage *= 2;
                else if (p_234563_1_.isExplosion())// explode add a lot
                    damage *= 4;
                int amount = (int) damage;
                if (amount != damage)
                    amount += player.getRNG().nextDouble() < (damage - amount) ? 1 : 0;
                if (amount <= 0)
                    return;
                for (ItemStack itemstack : player.getArmorInventoryList()) {
                    if (itemstack.isEmpty())
                        continue;
                    CompoundNBT cn = itemstack.getTag();
                    if (cn == null)
                        continue;
                    String inner = cn.getString("inner_cover");
                    if (inner.isEmpty())
                        continue;
                    if (cn.getBoolean("inner_bounded")) {
                        int dmg = cn.getInt("inner_damage");
                        if (dmg < itemstack.getDamage()) {
                            dmg = itemstack.getDamage();
                        }
                        dmg += amount;
                        if (dmg >= itemstack.getMaxDamage()) {
                            cn.remove("inner_cover");
                            cn.remove("inner_cover_tag");
                            cn.remove("inner_bounded");
                            cn.remove("inner_damage");
                            player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
                        } else cn.putInt("inner_damage", dmg);
                        continue;
                    }
                    CompoundNBT cnbt = cn.getCompound("inner_cover_tag");
                    int i = FHUtils.getEnchantmentLevel(Enchantments.UNBREAKING, cnbt);
                    int j = 0;
                    if (i > 0)
                        for (int k = 0; k < amount; ++k) {
                            if (UnbreakingEnchantment.negateDamage(itemstack, i, player.getRNG())) {
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
                        player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
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
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            Block growBlock = event.getPlacedBlock().getBlock();
            float temp = ChunkHeatData.getTemperature(event.getWorld(), event.getPos());
            if (growBlock instanceof IGrowable) {
                if (growBlock instanceof SaplingBlock) {
                    if (temp < -5) {
                    	player.sendStatusMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(-6)), true);
                    }
                } else if (growBlock instanceof FHCropBlock) {
                    int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(growTemp)), true);
                    }
                } else if (growBlock instanceof FHBerryBushBlock) {
                    int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(growTemp)), true);
                    }
                } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                    if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                        event.setCanceled(true);
                        player.sendStatusMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(WorldTemperature.HEMP_GROW_TEMPERATURE)), true);
                    }
                } else if (growBlock == Blocks.NETHERRACK) {

                } else if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE) {
                    event.setCanceled(true);
                    player.sendStatusMessage(TranslateUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE)), true);

                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        AddTempCommand.register(dispatcher);
        ClimateCommand.register(dispatcher);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START) {
            World world = event.world;
            if (!world.isRemote && world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) world;

                // Scheduled checks (e.g. town structures)
                SchedulerQueue.tickAll(serverWorld);

                // Town logic tick
                int i = 0;
                for (TeamDataHolder trd : FHTeamDataManager.INSTANCE.getAllData()) {
                    if (serverWorld.getDimensionKey().equals(trd.getData(SpecialDataTypes.GENERATOR_DATA).dimension)) {
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
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            Block growBlock = event.getBlock().getBlock();
            float temp = ChunkHeatData.getTemperature(event.getWorld(), event.getPos());
            if (growBlock instanceof FHCropBlock) {
                int growTemp = ((FHCropBlock) growBlock).getGrowTemperature() + WorldTemperature.BONEMEAL_TEMPERATURE;
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(growTemp)), true);
                }
            } else if (growBlock instanceof FHBerryBushBlock) {
                int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature() + WorldTemperature.BONEMEAL_TEMPERATURE;
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(growTemp)), true);
                }
            } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE) {
                    event.setCanceled(true);
                    player.sendStatusMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(WorldTemperature.HEMP_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE)), true);
                }
            } else if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE) {
                event.setCanceled(true);
                player.sendStatusMessage(TranslateUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE)), true);
            }
        }
    }
    @SubscribeEvent
    public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerWorld serverWorld = ((ServerPlayerEntity) event.getPlayer()).getServerWorld();
            PacketTarget currentPlayer=PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer());
            FHResearch.sendSyncPacket(currentPlayer);
            for(DataType type:DataType.types)
            	FHNetwork.send(currentPlayer,new FHDatapackSyncPacket(type));
            FHNetwork.send(currentPlayer,new FHResearchDataSyncPacket(ResearchDataAPI.getData((ServerPlayerEntity) event.getEntity())));
            FHCapabilities.CLIMATE_DATA.getCapability(serverWorld).ifPresent((cap) -> FHNetwork.send(currentPlayer,new FHClimatePacket(cap)));

            FHNetwork.send(currentPlayer, new WaypointSyncAllPacket((ServerPlayerEntity) event.getPlayer()));
            //System.out.println("=x-x=");
            //System.out.println(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS.getValue(new ResourceLocation(FHMain.MODID,"add_loot")));
        }
    }

    @SubscribeEvent
    public static void syncDataWhenDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerWorld serverWorld = ((ServerPlayerEntity) event.getPlayer()).getServerWorld();

            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                    new FHClimatePacket(WorldClimate.get(serverWorld)));
        }
    }
}
