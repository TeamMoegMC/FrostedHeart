/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.infrastructure.gen;

import com.simibubi.create.content.logistics.funnel.FunnelItem;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.model.generators.ModelFile;

public class FHBlockStateGen {
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleCubeAll(
            ResourceLocation texture) {
        return (c, p) -> p.simpleBlock(c.get(), p.models()
                .cubeAll(c.getName(), texture));
    }
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleCubeAll(
            String path) {
        return (c, p) -> p.simpleBlock(c.get(), p.models()
                .cubeAll(c.getName(), p.modLoc("block/" + path)));
    }

    // snow layered block state
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> snowLayered(
            String layerPath, String blockPath, String texturePath) {
        return (c, p) -> {
            SnowLayerBlock layer = (SnowLayerBlock) c.get();
            ResourceLocation texture = p.modLoc("block/" + texturePath);

            ModelFile[] models = new ModelFile[8];
            models[0] = p.models().withExistingParent(layerPath + "_height2", p.mcLoc("block/snow_height2"))
                    .texture("particle", texture)
                    .texture("texture", texture);
            models[1] = p.models().withExistingParent(layerPath + "_height4", p.mcLoc("block/snow_height4"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[2] = p.models().withExistingParent(layerPath + "_height6", p.mcLoc("block/snow_height6"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[3] = p.models().withExistingParent(layerPath + "_height8", p.mcLoc("block/snow_height8"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[4] = p.models().withExistingParent(layerPath + "_height10", p.mcLoc("block/snow_height10"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[5] = p.models().withExistingParent(layerPath + "_height12", p.mcLoc("block/snow_height12"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[6] = p.models().withExistingParent(layerPath + "_height14", p.mcLoc("block/snow_height14"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[7] = p.models().withExistingParent(blockPath, p.mcLoc("block/cube_all"))
                            .texture("all", texture);

            p.getVariantBuilder(layer)
                    .partialState().with(SnowLayerBlock.LAYERS, 1)
                    .modelForState().modelFile(models[0]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 2)
                    .modelForState().modelFile(models[1]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 3)
                    .modelForState().modelFile(models[2]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 4)
                    .modelForState().modelFile(models[3]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 5)
                    .modelForState().modelFile(models[4]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 6)
                    .modelForState().modelFile(models[5]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 7)
                    .modelForState().modelFile(models[6]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 8)
                    .modelForState().modelFile(models[7]).addModel();
        };
    }

    public static NonNullBiConsumer<DataGenContext<Item, BlockItem>, RegistrateItemModelProvider> itemModelLayered(
            String path, String texturePath) {
        return (c, p) -> {
            ResourceLocation texture = p.modLoc("block/" + texturePath);
            p.withExistingParent(path, p.mcLoc("block/snow_height2"))
                    .texture("particle", texture)
                    .texture("texture", texture);
        };
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> ruinedMachines() {
        return b -> b
                .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                        .requiresCorrectToolForDrops()
                        .strength(10, 10)
                        .sound(SoundType.METAL)
                        .noOcclusion())
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .tag(BlockTags.NEEDS_IRON_TOOL);
    }

}
