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

package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import com.teammoeg.frostedheart.block.cropblock.FHCropBlock;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.content.FHItems;
import com.teammoeg.frostedheart.nbt.FHNBT;
import com.teammoeg.frostedheart.network.ChunkUnwatchPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.resources.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import com.teammoeg.frostedheart.world.FHFeatures;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHForgeEvents {
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {

    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.WorldTickEvent event) {

    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        IReloadableResourceManager resourceManager = (IReloadableResourceManager) dataPackRegistries.getResourceManager();
        event.addListener(new FHRecipeReloadListener(dataPackRegistries));
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
                data = ChunkDataCache.SERVER.getOrCreate(chunkPos);
            } else {
                // This may happen before or after the chunk is watched and synced to client
                // Default to using the cache. If later the sync packet arrives it will update the same instance in the chunk capability and cache
                data = ChunkDataCache.CLIENT.getOrCreate(chunkPos);
            }
            event.addCapability(ChunkDataCapabilityProvider.KEY, data);
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        // Send an update packet to the client when watching the chunk
        ChunkPos pos = event.getPos();
        ChunkData chunkData = ChunkData.get(event.getWorld(), pos);
       // if (chunkData.getStatus() != ChunkData.Status.EMPTY) {
            PacketHandler.send(PacketDistributor.PLAYER.with(event::getPlayer), chunkData.getUpdatePacket());
        /*} else {
            // Chunk does not exist yet but it's queue'd for watch. Queue an update packet to be sent on chunk load
            ChunkDataCache.WATCH_QUEUE.enqueueUnloadedChunk(pos, event.getPlayer());
        }*/
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getWorld().isRemote() && !(event.getChunk() instanceof EmptyChunk)) {
            ChunkPos pos = event.getChunk().getPos();
            ChunkData.getCapability(event.getChunk()).ifPresent(data -> {
                ChunkDataCache.SERVER.update(pos, data);
                ChunkDataCache.WATCH_QUEUE.dequeueLoadedChunk(pos, data);
            });
            
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        // Clear server side chunk data cache
        if (!event.getWorld().isRemote() && !(event.getChunk() instanceof EmptyChunk)) {
        	
            ChunkDataCache.SERVER.remove(event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        // Send an update packet to the client when un-watching the chunk
        ChunkPos pos = event.getPos();
        PacketHandler.send(PacketDistributor.PLAYER.with(event::getPlayer), new ChunkUnwatchPacket(pos));
        ChunkDataCache.WATCH_QUEUE.dequeueChunk(pos, event.getPlayer());
    }

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
        ChunkData data = ChunkData.get(event.getWorld(), event.getPos());
        float temp = data.getTemperatureAtBlock(event.getPos());
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
            ChunkData data = ChunkData.get(event.getWorld(), event.getPos());
            float temp = data.getTemperatureAtBlock(event.getPos());
            if (growBlock instanceof FHCropBlock) {
                int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable").appendString(growTemp + "°C"), false);
                }
            } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable").appendString(WorldClimate.HEMP_GROW_TEMPERATURE + "°C"), false);
                }
            } else {
                event.setCanceled(true);
                player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable").appendString(WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE + "°C"), false);
            }
        }
    }

    //TODO create grow temperature mappings for every plant in the modpack
    @SubscribeEvent
    public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            Block growBlock = event.getPlacedBlock().getBlock();
            ChunkData data = ChunkData.get(event.getWorld(), event.getPos());
            float temp = data.getTemperatureAtBlock(event.getPos());
            if (growBlock instanceof IGrowable) {
                if (growBlock instanceof SaplingBlock) {
                    //TODO: allow planting trees now, maybe i will add some restrictions in the future
                } else if (growBlock instanceof FHCropBlock) {
                    int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable").appendString(growTemp + "°C"), false);
                    }
                } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                    if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
                        event.setCanceled(true);
                        player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable").appendString(WorldClimate.HEMP_GROW_TEMPERATURE + "°C"), false);
                    }
                } else {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable").appendString(WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE + "°C"), false);
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
            event.getPlayer().inventory.armorInventory.set(3, new ItemStack(FHItems.wool_hat));
            event.getPlayer().inventory.armorInventory.set(2, new ItemStack(FHItems.wool_jacket));
            event.getPlayer().inventory.armorInventory.set(1, new ItemStack(FHItems.wool_pants));
            event.getPlayer().inventory.armorInventory.set(0, new ItemStack(FHItems.wool_boots));
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
}
