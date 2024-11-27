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

package com.teammoeg.frostedheart.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.gen.FHRegistrateTags;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper exHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
        PackOutput output = gen.getPackOutput();
        gen.addProvider(event.includeServer(), new FHBlockTagProvider(gen, FHMain.MODID, exHelper, event.getLookupProvider()));
        gen.addProvider(event.includeServer(), new FHRecipeProvider(gen));
        gen.addProvider(event.includeServer(), new FHMultiblockStatesProvider(gen, FHMain.MODID, exHelper));
        gen.addProvider(event.includeClient(), new FHItemModelProvider(gen, FHMain.MODID, exHelper));
        gen.addProvider(event.includeClient(), new FHBlockModelProvider(gen, FHMain.MODID, exHelper));
        gen.addProvider(event.includeServer(), new FHItemTagProvider(gen, FHMain.MODID, exHelper, lookup));
        gen.addProvider(event.includeServer(), new FHLootTableProvider(output));
        gen.addProvider(event.includeServer(), new FHLangProvider(output, FHMain.MODID, "en_us"));
        for (final DataProvider provider : makeProviders(output, lookup, exHelper))
            gen.addProvider(event.includeServer(), provider);

        addExtraRegistrateData();
    }

    public static List<DataProvider> makeProviders(PackOutput output, CompletableFuture<HolderLookup.Provider> vanillaRegistries, ExistingFileHelper exFiles) {
        final RegistrySetBuilder registryBuilder = new RegistrySetBuilder();
//        registryBuilder.add(Registries.CONFIGURED_FEATURE, ctx -> bootstrapConfiguredFeatures(ctx, registrations));
//        registryBuilder.add(Registries.PLACED_FEATURE, ctx -> bootstrapPlacedFeatures(ctx, registrations));
//        registryBuilder.add(ForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> bootstrapBiomeModifiers(ctx, registrations));
        registryBuilder.add(Registries.DAMAGE_TYPE, FHDamageTypeProvider::bootstrap);
        return List.of(
                new DatapackBuiltinEntriesProvider(output, vanillaRegistries, registryBuilder, Set.of(FHMain.MODID)),
                new FHDamageTypeTagProvider(output, vanillaRegistries.thenApply(r -> append(r, registryBuilder)), exFiles)
        );
    }

    private static HolderLookup.Provider append(HolderLookup.Provider original, RegistrySetBuilder builder)
    {
        return builder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), original);
    }

    private static void addExtraRegistrateData() {
        FHRegistrateTags.addGenerators();
        FHMain.FH_REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
//            provider.add("itemGroup.frostedheart", "Frosted Heart");
            BiConsumer<String, String> langConsumer = provider::add;
            provideDefaultLang("en_us", langConsumer);
        });
    }

    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/frostedheart/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null) {
            throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            consumer.accept(key, value);
        }
    }
}
