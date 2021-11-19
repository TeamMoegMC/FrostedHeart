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

package com.teammoeg.frostedheart.events;

import javax.annotation.Nonnull;

import com.mojang.brigadier.CommandDispatcher;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.command.AddTempCommand;
import com.teammoeg.frostedheart.content.agriculture.FHBerryBushBlock;
import com.teammoeg.frostedheart.content.agriculture.FHCropBlock;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.data.FHDataReloadManager;
import com.teammoeg.frostedheart.network.FHDatapackSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.resources.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import com.teammoeg.frostedheart.util.FHDamageSources;
import com.teammoeg.frostedheart.util.FHNBT;
import com.teammoeg.frostedheart.world.FHFeatures;

import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {

    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.WorldTickEvent event) {

    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        //IReloadableResourceManager resourceManager = (IReloadableResourceManager) dataPackRegistries.getResourceManager();
        event.addListener(new FHRecipeReloadListener(dataPackRegistries));
        event.addListener(FHDataReloadManager.INSTANCE);
//            resourceManager.addReloadListener(ChunkCacheInvalidationReloaderListener.INSTANCE);
    }

    @SubscribeEvent
    public static void addReloadListenersLowest(AddReloadListenerEvent event) {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        event.addListener(new FHRecipeCachingReloadListener(dataPackRegistries));
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesWorld(AttachCapabilitiesEvent<World> event) {

    }

    @SubscribeEvent
    public static void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<Chunk> event) {
        if (!event.getObject().isEmpty()) {
            World world = event.getObject().getWorld();
            ChunkPos chunkPos = event.getObject().getPos();
            ChunkData data;
            if (!world.isRemote) {
                // Chunk was created on server thread.
                // 1. If this was due to world gen, it won't have any cap data. This is where we clear the world gen cache and attach it to the chunk
                // 2. If this was due to chunk loading, the caps will be deserialized from NBT after this event is posted. Attach empty data here
                // 下面这段代码导致每次attach到Chunk的data都是new出来的默认值。
                // 因为我们没有世界生成阶段的温度生成，所以暂且注掉
                // data = ChunkDataCache.WORLD_GEN.remove(chunkPos);
                // if (data == null) {
                //    data = new ChunkData(chunkPos);
                // }
               // data = ChunkDataCache.SERVER.getOrCreate(chunkPos);
            	if(!event.getCapabilities().containsKey(ChunkDataCapabilityProvider.KEY))
            		event.addCapability(ChunkDataCapabilityProvider.KEY,new ChunkData(chunkPos));
            }	

        }
    }


    /*@SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getWorld().isRemote() && !(event.getChunk() instanceof EmptyChunk)) {
            ChunkPos pos = event.getChunk().getPos();
            ChunkData.getCapability(event.getChunk()).ifPresent(data -> {
                ChunkDataCache.SERVER.update(pos, data);
                // ChunkDataCache.WATCH_QUEUE.dequeueLoadedChunk(pos, data);
            });

        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        // Clear server side chunk data cache
        if (!event.getWorld().isRemote() && !(event.getChunk() instanceof EmptyChunk)) {
            ChunkDataCache.SERVER.remove(event.getChunk().getPos());
        }
    }*/


    @SubscribeEvent
    public static void addOreGenFeatures(BiomeLoadingEvent event) {
        if (event.getName() != null)
            if (event.getCategory() != Biome.Category.NETHER && event.getCategory() != Biome.Category.THEEND) {
                for (ConfiguredFeature feature : FHFeatures.FH_ORES)
                    event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
            }
    }

    @SubscribeEvent
    public static void beforeCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        Block growBlock = event.getState().getBlock();
        float temp = ChunkData.getTemperature(event.getWorld(), event.getPos());
        if (growBlock instanceof FHCropBlock) {
            event.setResult(Event.Result.DEFAULT);
        } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
            if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }
        } else {
            if (temp < WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE) {
                // Set back to default state, might not be necessary
                if (event.getWorld().getBlockState(event.getPos()) != growBlock.getDefaultState() && event.getWorld().getRandom().nextInt(3) == 0) {
                    event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onUseBoneMeal(BonemealEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            PlayerEntity player = event.getPlayer();
            Block growBlock = event.getBlock().getBlock();
            float temp = ChunkData.getTemperature(event.getWorld(), event.getPos());
            if (growBlock instanceof FHCropBlock) {
                int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",growTemp), false);
                }
            } else if (growBlock instanceof FHBerryBushBlock) {
                int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",growTemp), false);
                }
            } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",WorldClimate.HEMP_GROW_TEMPERATURE), false);
                }
            } else if(temp<WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE){
                event.setCanceled(true);
                player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE), false);
            }
        }
    }

    //TODO create grow temperature mappings for every plant in the modpack
    @SubscribeEvent
    public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            Block growBlock = event.getPlacedBlock().getBlock();
            float temp = ChunkData.getTemperature(event.getWorld(), event.getPos());
            if (growBlock instanceof IGrowable) {
                if (growBlock instanceof SaplingBlock) {
                    //TODO: allow planting trees now, maybe i will add some restrictions in the future
                } else if (growBlock instanceof FHCropBlock) {
                    int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",growTemp), false);
                    }
                } else if (growBlock instanceof FHBerryBushBlock){
                    int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",growTemp), false);
                    }
                } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                    if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
                        event.setCanceled(true);
                        player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",WorldClimate.HEMP_GROW_TEMPERATURE), false);
                    }
                } else if(temp<WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE){
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE), false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void addManualToPlayer(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
        CompoundNBT nbt = event.getPlayer().getPersistentData();
        CompoundNBT persistent;

        if (nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        } else {
            nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
        }
        if (!persistent.contains(FHNBT.FIRST_LOGIN_GIVE_MANUAL)) {
            persistent.putBoolean(FHNBT.FIRST_LOGIN_GIVE_MANUAL, false);
            event.getPlayer().inventory.addItemStackToInventory(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ftbquests", "book"))));
            event.getPlayer().inventory.armorInventory.set(3, FHNBT.ArmorNBT(new ItemStack(Items.IRON_HELMET).setDisplayName(new TranslationTextComponent(  "itemname.frostedheart.start_head"))));
            event.getPlayer().inventory.armorInventory.set(2, FHNBT.ArmorNBT(new ItemStack(Items.IRON_CHESTPLATE).setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_chest"))));
            event.getPlayer().inventory.armorInventory.set(1, FHNBT.ArmorNBT(new ItemStack(Items.IRON_LEGGINGS).setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_leg"))));
            event.getPlayer().inventory.armorInventory.set(0, FHNBT.ArmorNBT(new ItemStack(Items.IRON_BOOTS).setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_foot"))));
            ItemStack breads = new ItemStack(Items.BREAD);
            breads.setCount(16);
            event.getPlayer().inventory.addItemStackToInventory(breads);
        }
    }

    //    @SubscribeEvent
//    public static void addBaseNutritionOnFirstLogin(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
//        CompoundNBT nbt = event.getPlayer().getPersistentData();
//        CompoundNBT persistent;
//
//        if (nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
//            persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
//        } else {
//            nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
//        }
//        if (!persistent.contains(FHNBT.FIRST_LOGIN_GIVE_NUTRITION)) {
//            persistent.putBoolean(FHNBT.FIRST_LOGIN_GIVE_NUTRITION, false);
//            if (ModList.get().isLoaded("diet") && event.getPlayer().getServer() != null && event.getPlayer().isServerWorld()) {
//                event.getPlayer().getServer().getCommandManager().handleCommand(event.getPlayer().getCommandSource(), "/diet set @s fruits 0.75");
//                event.getPlayer().getServer().getCommandManager().handleCommand(event.getPlayer().getCommandSource(), "/diet set @s grains 0.75");
//                event.getPlayer().getServer().getCommandManager().handleCommand(event.getPlayer().getCommandSource(), "/diet set @s proteins 0.75");
//                event.getPlayer().getServer().getCommandManager().handleCommand(event.getPlayer().getCommandSource(), "/diet set @s sugars 0.75");
//                event.getPlayer().getServer().getCommandManager().handleCommand(event.getPlayer().getCommandSource(), "/diet set @s vegetables 0.75");
//            }
//        }
//    }
    @SubscribeEvent
    public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity)
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()), new FHDatapackSyncPacket());
    }

    @SubscribeEvent
    public static void setKeepInventory(FMLServerStartedEvent event) {
        if (FHConfig.SERVER.alwaysKeepInventory.get()) {
            for (ServerWorld world : event.getServer().getWorlds()) {
                world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, event.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void punishEatingRawMeat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof ServerPlayerEntity && event.getItem().getItem().getTags().contains(FHMain.rl("raw_food"))) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            player.addPotionEffect(new EffectInstance(Effects.POISON, 400, 1));
            player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 400, 1));
            if (ModList.get().isLoaded("diet") && player.getServer() != null) {
                player.getServer().getCommandManager().handleCommand(player.getCommandSource(), "/diet subtract @s proteins 0.01");
            }
            player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.eaten_poisonous_food"), false);
        }
    }

    @SubscribeEvent
    public static void eatingFood(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof ServerPlayerEntity) {
            ItemStack is = event.getItem();
            Item it = event.getItem().getItem();
            ITempAdjustFood adj = null;
            //System.out.println(it.getRegistryName());
            double tspeed=FHConfig.SERVER.tempSpeed.get();
            if (it instanceof ITempAdjustFood) {
                adj = (ITempAdjustFood) it;
            } else {
                adj = FHDataManager.getFood(is);
            }
            if (adj != null) {
                float current = TemperatureCore.getBodyTemperature((ServerPlayerEntity) event.getEntityLiving());
                float max = adj.getMaxTemp(event.getItem());
                float min = adj.getMinTemp(event.getItem());
                float heat = adj.getHeat(event.getItem());
                if(heat>1)
                	event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPERTHERMIA,(heat)*2);
                else if(heat<-1) 
                	event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPOTHERMIA,(heat)*2);
                if (heat > 0) {
                    if (current >= max) return;
                    current += heat*tspeed;
                    if (current > max) current = max;
                } else {
                    if (current <= min) return;
                    current += heat*tspeed;
                    if (current <= min) return;
                }
                TemperatureCore.setBodyTemperature((ServerPlayerEntity) event.getEntityLiving(), current);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        AddTempCommand.register(dispatcher);
    }

//    @SubscribeEvent
//    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
//        ToolType Tool = event.getState().getHarvestTool();
//        if (Tool != null) {
//            if (!FHTags.Blocks.ALWAYS_BREAKABLE.contains(event.getState().getBlock()))
//                if (event.getPlayer().getHeldItemMainhand().getHarvestLevel(Tool, event.getPlayer(), event.getState()) == -1)
//                    event.setNewSpeed(0);
//        }
//    }
}
