package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.client.ClientProxy;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlock;
import blusunrize.immersiveengineering.common.blocks.stone.StoneMultiBlock;
import blusunrize.immersiveengineering.common.crafting.RecipeReloadListener;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.common.GeneratorContainer;
import com.teammoeg.frostedheart.common.GeneratorMultiblock;
import com.teammoeg.frostedheart.client.GeneratorScreen;
import com.teammoeg.frostedheart.common.GeneratorTileEntity;
import com.teammoeg.frostedheart.common.block.BlockItemFH;
import com.teammoeg.frostedheart.common.block.FHBaseBlock;
import com.teammoeg.frostedheart.crafting.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.crafting.FHRecipeReloadListener;
import com.teammoeg.frostedheart.data.FHRecipeProvider;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FHMain.MODID)
public class FHMain {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "frostedheart";

    public FHMain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::gatherData);

        MinecraftForge.EVENT_BUS.register(this);

        // Register recipe serializing
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::addReloadListenersLowest);
        FHRecipeSerializers.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Init block
        FHBlocks.generator = new StoneMultiBlock<GeneratorTileEntity>("generator", FHTileTypes.GENERATOR) {
            @Override
            public ResourceLocation createRegistryName() {
                return new ResourceLocation(FHMain.MODID, name);
            }
        };

        Block.Properties stoneDecoProps = Block.Properties.create(Material.ROCK)
                .sound(SoundType.STONE)
                .setRequiresTool()
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2, 10);

        FHBlocks.generator_brick = new FHBaseBlock("generator_brick", stoneDecoProps, BlockItemFH::new);

        // Init multiblocks
        FHMultiblocks.GENERATOR = new GeneratorMultiblock();

        // Register tile types
        FHTileTypes.REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Register containers
        GuiHandler.register(GeneratorTileEntity.class, new ResourceLocation(MODID, "generator"), GeneratorContainer::new);

        // Register screens
        ((ClientProxy) ImmersiveEngineering.proxy).registerScreen(new ResourceLocation(MODID, "generator"), GeneratorScreen::new);

        // Register recipe types
        DeferredWorkQueue.runLater(FHRecipeTypes::registerRecipeTypes);
    }

    // data generator event
    public void gatherData(GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();
        if (event.includeServer())
        {
            gen.addProvider(new FHRecipeProvider(gen));
        }
    }

    public void addReloadListeners(AddReloadListenerEvent event)
    {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        event.addListener(new FHRecipeReloadListener(dataPackRegistries));
    }

    public void addReloadListenersLowest(AddReloadListenerEvent event)
    {
        event.addListener(new FHRecipeCachingReloadListener(event.getDataPackRegistries()));
    }

    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new FHRecipeReloadListener(null));
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

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {

        }
    }
}
