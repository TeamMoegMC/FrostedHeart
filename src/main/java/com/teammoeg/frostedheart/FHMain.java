package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.client.ClientProxy;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.stereowalker.survive.Survive;
import com.teammoeg.frostedheart.client.screen.GeneratorScreen;
import com.teammoeg.frostedheart.common.block.cropblock.FHCropBlock;
import com.teammoeg.frostedheart.listener.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.listener.FHRecipeReloadListener;
import com.teammoeg.frostedheart.network.ChunkUnwatchPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCapability;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;

import static com.teammoeg.frostedheart.FHContent.*;
import static net.minecraft.util.text.TextFormatting.*;

@Mod(FHMain.MODID)
public class FHMain {

    private static final Logger LOGGER = LogManager.getLogger(FHMain.MODNAME);
    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";

    public static ItemGroup itemGroup = new ItemGroup(MODID)
    {
        @Override
        @Nonnull
        public ItemStack createIcon()
        {
            return new ItemStack(FHContent.Blocks.generator_core_t1.asItem());
        }
    };

    public FHMain() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(this);

        modBus.addListener(this::setup);
        modBus.addListener(this::doClientStuff);
        modBus.addListener(this::processIMC);
        modBus.addListener(this::enqueueIMC);

        // Register recipe serializers
        FHRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        // Register tile types
        FHTileTypes.REGISTER.register(modBus);
        // Register recipe types
        DeferredWorkQueue.runLater(FHRecipeTypes::registerRecipeTypes);
        // Register network packets
        PacketHandler.register();
        // Populate FH content
        FHContent.populate();
        // Register FH content
        FHContent.registerAll();
    }

    public void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new FHRecipeReloadListener(null));
        ChunkDataCapability.setup();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {

    }

    private void enqueueIMC(final InterModEnqueueEvent event) {

    }

    private void processIMC(final InterModProcessEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {

    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            for (Block block : registeredFHBlocks) {
                try {
                    event.getRegistry().register(block);
                } catch (Throwable e) {
                    LOGGER.error("Failed to register a block. ({})", block);
                    throw e;
                }
            }
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            for (Item item : registeredFHItems) {
                try {
                    event.getRegistry().register(item);
                } catch (Throwable e) {
                    LOGGER.error("Failed to register an item. ({}, {})", item, item.getRegistryName());
                    throw e;
                }
            }
        }

        @SubscribeEvent
        public static void registerFluids(RegistryEvent.Register<Fluid> event) {
            for (Fluid fluid : registeredFHFluids) {
                try {
                    event.getRegistry().register(fluid);
                } catch (Throwable e) {
                    LOGGER.error("Failed to register a fluid. ({}, {})", fluid, fluid.getRegistryName());
                    throw e;
                }
            }
        }

        @SubscribeEvent
        public static void onFeatureRegistry(RegistryEvent.Register<Feature<?>> event) {
            event.getRegistry().register(FHFeatures.FHORE.setRegistryName(FHMain.MODID, "fhore"));
        }

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void onRenderTypeSetup(FMLClientSetupEvent event) {
            // Register screens
            ((ClientProxy) ImmersiveEngineering.proxy).registerScreen(new ResourceLocation(FHMain.MODID, "generator"), GeneratorScreen::new);
            // Register translucent render type
            RenderTypeLookup.setRenderLayer(FHContent.Blocks.rye_block, RenderType.getCutoutMipped());
            RenderTypeLookup.setRenderLayer(FHContent.Multiblocks.generator, RenderType.getCutoutMipped());
        }
    }

    @Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
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
                event.addCapability(ChunkDataCapability.KEY, data);
            }
        }

        @SubscribeEvent
        public static void onChunkWatch(ChunkWatchEvent.Watch event) {
            // Send an update packet to the client when watching the chunk
            ChunkPos pos = event.getPos();
            ChunkData chunkData = ChunkData.get(event.getWorld(), pos);
            if (chunkData.getStatus() != ChunkData.Status.EMPTY) {
                PacketHandler.send(PacketDistributor.PLAYER.with(event::getPlayer), chunkData.getUpdatePacket());
            } else {
                // Chunk does not exist yet but it's queue'd for watch. Queue an update packet to be sent on chunk load
                ChunkDataCache.WATCH_QUEUE.enqueueUnloadedChunk(pos, event.getPlayer());
            }
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
            if (!(event.getState().getBlock() instanceof FHCropBlock)) {
                event.setResult(Event.Result.DENY);
                ChunkData data = ChunkData.get(event.getWorld(), event.getPos());
                float temp = data.getTemperatureAtBlock(event.getPos());
                if (temp < 20) {
                    event.getWorld().setBlockState(event.getPos(), Blocks.DEAD_BUSH.getDefaultState(), 2);
                }
            }

        }
    }

    @Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @SubscribeEvent
        public static void onRenderGameOverlayText(RenderGameOverlayEvent.Text event) {
            Minecraft mc = Minecraft.getInstance();
            List<String> list = event.getRight();
            if (mc.world != null && mc.gameSettings.showDebugInfo) {
                BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
                if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                    list.add("");
                    list.add(AQUA + FHMain.MODNAME);
                    ChunkData data = ChunkData.get(mc.world, pos);
                    if (data.getStatus().isAtLeast(ChunkData.Status.CLIENT)) {
                        list.add(GRAY + I18n.format("frostedheart.tooltip.f3_average_temperature", WHITE + String.format("%.1f", data.getTemperatureAtBlock(pos))));
                    }
                } else {
                    list.add(GRAY + I18n.format("frostedheart.tooltip.f3_invalid_chunk_data"));
                }
            }
        }

        @SubscribeEvent
        public static void renderGameOverlay(RenderGameOverlayEvent event) {
            Minecraft mc = Minecraft.getInstance();
            IngameGui gui = mc.ingameGUI;
            FontRenderer font = gui.getFontRenderer();
            ResourceLocation tempBarLocation = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_bar.png");

            mc.getProfiler().startSection("frostedheart_temperature");
            mc.getTextureManager().bindTexture(tempBarLocation);

            if (Minecraft.isGuiEnabled() && mc.playerController.gameIsSurvivalOrAdventure()) {
                gui.blit(event.getMatrixStack(), 0, 0, 0, 0, 148, 34);
                if (mc.world != null) {
                    BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
                    if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                        ChunkData data = ChunkData.get(mc.world, pos);
                        if (data.getStatus().isAtLeast(ChunkData.Status.CLIENT)) {
                            font.drawString(event.getMatrixStack(), String.format("%.1f", data.getTemperatureAtBlock(pos)), 5, 14, 0);
                            font.drawString(event.getMatrixStack(), I18n.format("gui.frostedheart.temperature.desc"), 33, 8, -1);
                        }
                    }
                }
            }

            mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
            mc.getProfiler().endSection();
        }
    }
}
