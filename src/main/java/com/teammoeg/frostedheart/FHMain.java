package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.capability.TempForecastCapabilityProvider;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.compat.EleCompat;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.annotation.Nonnull;

@Mod(FHMain.MODID)
public class FHMain {

    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";

    public static final ItemGroup itemGroup = new ItemGroup(MODID) {
        @Override
        @Nonnull
        public ItemStack createIcon()
        {
            return new ItemStack(FHContent.Blocks.generator_core_t1.asItem());
        }
    };

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public FHMain() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();

        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);

        // Register recipe serializers
        FHRecipeSerializers.RECIPE_SERIALIZERS.register(mod);
        // Register tile types
        FHTileTypes.REGISTER.register(mod);
        // Register container
        FHTileTypes.CONTAINERS.register(mod);
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
        ChunkDataCapabilityProvider.setup();
        TempForecastCapabilityProvider.setup();
        EleCompat.setup();
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {

    }

    private void processIMC(final InterModProcessEvent event) {

    }
}
