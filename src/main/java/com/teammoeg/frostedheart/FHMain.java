package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.client.ClientProxy;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.stereowalker.survive.item.SItems;
import com.teammoeg.frostedheart.client.screen.CrucibleScreen;
import com.teammoeg.frostedheart.client.screen.ElectrolyzerScreen;
import com.teammoeg.frostedheart.client.screen.GeneratorScreen;
import com.teammoeg.frostedheart.common.block.cropblock.FHCropBlock;
import com.teammoeg.frostedheart.listener.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.listener.FHRecipeReloadListener;
import com.teammoeg.frostedheart.network.ChunkUnwatchPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.util.UV4i;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCapability;
import com.teammoeg.frostedheart.world.noise.Vec4;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.KelpBlock;
import net.minecraft.block.KelpTopBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.teammoeg.frostedheart.FHContent.*;
import static net.minecraft.util.text.TextFormatting.*;

@Mod(FHMain.MODID)
public class FHMain {

    private static final Logger LOGGER = LogManager.getLogger(FHMain.MODNAME);
    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";
    public static String FIRST_NBT = "first";

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
        // Register container
        FHTileTypes.CONTAINERS.register(modBus);
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
            ((ClientProxy) ImmersiveEngineering.proxy).registerScreen(new ResourceLocation(FHMain.MODID, "crucible"), CrucibleScreen::new);
            ScreenManager.registerFactory(FHTileTypes.ELECTROLYZER_CONTAINER.get(), ElectrolyzerScreen::new);
            // Register translucent render type
            RenderTypeLookup.setRenderLayer(FHContent.Blocks.rye_block, RenderType.getCutoutMipped());
            RenderTypeLookup.setRenderLayer(FHContent.Blocks.electrolyzer, RenderType.getCutoutMipped());
            RenderTypeLookup.setRenderLayer(FHContent.Multiblocks.generator, RenderType.getCutoutMipped());
            RenderTypeLookup.setRenderLayer(FHContent.Multiblocks.crucible, RenderType.getCutoutMipped());
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
            if (!(event.getState().getBlock() instanceof FHCropBlock) && !(event.getState().getBlock() instanceof KelpBlock) && !(event.getState().getBlock() instanceof KelpTopBlock)) {
                event.setResult(Event.Result.DENY);
                ChunkData data = ChunkData.get(event.getWorld(), event.getPos());
                float temp = data.getTemperatureAtBlock(event.getPos());
                if (temp < 20) {
                    event.getWorld().setBlockState(event.getPos(), Blocks.DEAD_BUSH.getDefaultState(), 2);
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
            if (!persistent.contains(FIRST_NBT)) {
                persistent.putBoolean(FIRST_NBT, false);

                event.getPlayer().inventory.addItemStackToInventory(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ftbquests", "book"))));
                event.getPlayer().inventory.armorInventory.set(3, new ItemStack(SItems.WOOL_HAT));
                event.getPlayer().inventory.armorInventory.set(2, new ItemStack(SItems.WOOL_JACKET));
                event.getPlayer().inventory.armorInventory.set(1, new ItemStack(SItems.WOOL_PANTS));
                event.getPlayer().inventory.armorInventory.set(0, new ItemStack(SItems.WOOL_BOOTS));
            }
        }

        @SubscribeEvent
        public static void setKeepInventory(FMLServerStartedEvent event) {
            for (ServerWorld world : event.getServer().getWorlds()) {
                world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, event.getServer());
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
            mc.getProfiler().startSection("frostedheart_temperature");
            if (Minecraft.isGuiEnabled() && mc.playerController.gameIsSurvivalOrAdventure() && mc.world != null) {
                BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
                if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                    ChunkData data = ChunkData.get(mc.world, pos);
                    if (data.getStatus().isAtLeast(ChunkData.Status.CLIENT)) {
                        int temperature = (int) data.getTemperatureAtBlock(pos);
                        renderTemp(event.getMatrixStack(), mc, temperature, true);
                    }
                }
            }

            mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
            mc.getProfiler().endSection();

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            RenderSystem.disableAlphaTest();
        }

        private static void renderTemp(MatrixStack stack, Minecraft mc, int temp, boolean celsius) {
            UV4i unitUV = celsius ? new UV4i(0, 25, 13, 34) : new UV4i(13, 25, 26, 34);
            UV4i signUV = temp >= 0 ? new UV4i(61, 17, 68, 24) : new UV4i(68, 17, 75, 24);
            int decimal = 0;
            int integer = Math.abs(temp);

            ResourceLocation digits = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/digits.png");
            ResourceLocation moderate = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/moderate.png");
            ResourceLocation chilly = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/chilly.png");
            ResourceLocation cold = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/cold.png");
            ResourceLocation frigid = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/frigid.png");
            ResourceLocation hadean = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/hadean.png");

            // draw orb
            if (temp > 0) {
                mc.getTextureManager().bindTexture(moderate);
            } else if (temp > -20) {
                mc.getTextureManager().bindTexture(chilly);
            } else if (temp > -40) {
                mc.getTextureManager().bindTexture(cold);
            } else if (temp > -80) {
                mc.getTextureManager().bindTexture(frigid);
            } else {
                mc.getTextureManager().bindTexture(hadean);
            }
            IngameGui.blit(stack, 0, 0, 0, 0, 36, 36, 36, 36);

            // draw temperature
            mc.getTextureManager().bindTexture(digits);
            // sign and unit
            IngameGui.blit(stack, 1, 12, signUV.x, signUV.y, signUV.w, signUV.h, 100, 34);
            IngameGui.blit(stack, 11, 24, unitUV.x, unitUV.y, unitUV.w, unitUV.h, 100, 34);
            // digits
            ArrayList<UV4i> uv4is = getIntegerDigitUVs(integer);
            UV4i decUV = getDecDigitUV(decimal);
            if (uv4is.size() == 1) {
                UV4i uv1 = uv4is.get(0);
                IngameGui.blit(stack, 13, 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
                IngameGui.blit(stack, 25, 16, decUV.x, decUV.y, decUV.w, decUV.h, 100, 34);
            } else if (uv4is.size() == 2) {
                UV4i uv1 = uv4is.get(0), uv2 = uv4is.get(1);
                IngameGui.blit(stack, 8, 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
                IngameGui.blit(stack, 18, 7, uv2.x, uv2.y, uv2.w, uv2.h, 100, 34);
                IngameGui.blit(stack, 28, 16, decUV.x, decUV.y, decUV.w, decUV.h, 100, 34);
            } else if (uv4is.size() == 3) {
                UV4i uv1 = uv4is.get(0), uv2 = uv4is.get(1), uv3 = uv4is.get(2);
                IngameGui.blit(stack, 7, 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
                IngameGui.blit(stack, 14, 7, uv2.x, uv2.y, uv2.w, uv2.h, 100, 34);
                IngameGui.blit(stack, 24, 7, uv3.x, uv3.y, uv3.w, uv3.h, 100, 34);
            }
        }

        private static ArrayList<UV4i> getIntegerDigitUVs(int digit) {
            ArrayList<UV4i> rtn = new ArrayList<>();
            UV4i v1, v2, v3;
            if (digit / 10 == 0) { // len = 1
                int firstDigit = digit; if (firstDigit == 0) firstDigit += 10;
                v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
                rtn.add(v1);
            } else if (digit / 10 < 10) { // len = 2
                int firstDigit = digit / 10; if (firstDigit == 0) firstDigit += 10;
                int secondDigit = digit % 10; if (secondDigit == 0) secondDigit += 10;
                v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
                v2 = new UV4i(10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
                rtn.add(v1);
                rtn.add(v2);
            } else { // len = 3
                int thirdDigit = digit % 10; if (thirdDigit == 0) thirdDigit += 10;
                int secondDigit = digit / 10; if (secondDigit == 0) secondDigit += 10;
                int firstDigit = digit / 100; if (firstDigit == 0) firstDigit += 10;
                v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
                v2 = new UV4i(10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
                v3 = new UV4i(10 * (thirdDigit - 1), 0, 10 * thirdDigit, 17);
                rtn.add(v1);
                rtn.add(v2);
                rtn.add(v3);
            }
            return rtn;
        }

        private static UV4i getDecDigitUV(int dec) {
            return new UV4i(6 * (dec - 1), 17, 6 * dec, 25);
        }
    }
}
