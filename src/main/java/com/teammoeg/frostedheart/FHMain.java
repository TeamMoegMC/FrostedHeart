package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.client.ClientProxy;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.client.GeneratorScreen;
import com.teammoeg.frostedheart.common.block.FHBaseBlock;
import com.teammoeg.frostedheart.common.block.FHBlockItem;
import com.teammoeg.frostedheart.common.block.GeneratorCoreBlock;
import com.teammoeg.frostedheart.common.block.GeneratorMultiblockBlock;
import com.teammoeg.frostedheart.common.block.multiblock.GeneratorMultiblock;
import com.teammoeg.frostedheart.common.container.GeneratorContainer;
import com.teammoeg.frostedheart.common.tile.GeneratorTileEntity;
import com.teammoeg.frostedheart.common.util.CacheInvalidationListener;
import com.teammoeg.frostedheart.crafting.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.crafting.FHRecipeReloadListener;
import com.teammoeg.frostedheart.network.ChunkUnwatchPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCapability;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static net.minecraft.util.text.TextFormatting.*;
import static net.minecraft.util.text.TextFormatting.GRAY;

@Mod(FHMain.MODID)
public class FHMain {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";

    public FHMain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);

        // Register recipe serializing
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::addReloadListenersLowest);
        FHRecipeSerializers.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Init block
        FHBlocks.generator = new GeneratorMultiblockBlock("generator", FHTileTypes.GENERATOR_T1);

        Block.Properties stoneDecoProps = Block.Properties.create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10);

        FHBlocks.generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, FHBlockItem::new);
        FHBlocks.generator_core_t1 = new GeneratorCoreBlock("generator_core_t1", stoneDecoProps, FHBlockItem::new);
        FHBlocks.generator_amplifier_r1 = new FHBaseBlock("generator_amplifier_r1", stoneDecoProps, FHBlockItem::new);

        // Init multiblocks
        FHMultiblocks.GENERATOR = new GeneratorMultiblock();
        MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR);

        // Register tile types
        FHTileTypes.REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Register containers
        GuiHandler.register(GeneratorTileEntity.class, new ResourceLocation(MODID, "generator"), GeneratorContainer::new);

        // Register screens
        ((ClientProxy) ImmersiveEngineering.proxy).registerScreen(new ResourceLocation(MODID, "generator"), GeneratorScreen::new);

        // Register recipe types
        DeferredWorkQueue.runLater(FHRecipeTypes::registerRecipeTypes);

        // Register packets
        PacketHandler.init();
    }

    public void addReloadListeners(AddReloadListenerEvent event) {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        event.addListener(new FHRecipeReloadListener(dataPackRegistries));
    }

    public void addReloadListenersLowest(AddReloadListenerEvent event) {
        event.addListener(new FHRecipeCachingReloadListener(event.getDataPackRegistries()));
    }

    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new FHRecipeReloadListener(null));
        ChunkDataCapability.setup();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {

    }

    private void processIMC(final InterModProcessEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {

    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }


    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {

        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
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
                    data = ChunkDataCache.WORLD_GEN.remove(chunkPos);
                    if (data == null) {
                        data = new ChunkData(chunkPos);
                    }
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
        public static void addReloadListeners(AddReloadListenerEvent event) {
            IReloadableResourceManager resourceManager = (IReloadableResourceManager) event.getDataPackRegistries().getResourceManager();
            resourceManager.addReloadListener(CacheInvalidationListener.INSTANCE);
        }

        @SubscribeEvent
        public static void beforeServerStart(FMLServerAboutToStartEvent event) {
            CacheInvalidationListener.INSTANCE.invalidateAll();
        }

        @SubscribeEvent
        public static void onServerStopped(FMLServerStoppedEvent event) {
            CacheInvalidationListener.INSTANCE.invalidateAll();
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
                        list.add(GRAY + I18n.format("frostedheart.tooltip.f3_average_temperature", WHITE + String.format("%.1f", data.getAverageTemp(pos))));
                    }
                } else {
                    list.add(GRAY + I18n.format("frostedheart.tooltip.f3_invalid_chunk_data"));
                }
            }
        }
    }

}
