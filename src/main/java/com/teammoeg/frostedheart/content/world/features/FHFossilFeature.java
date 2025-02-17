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

package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.FossilFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

import java.util.List;
import java.util.Objects;

public class FHFossilFeature extends Feature<FossilFeatureConfiguration> {
    public FHFossilFeature(Codec<FossilFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FossilFeatureConfiguration> pContext) {
        RandomSource random = pContext.random();
        WorldGenLevel worldGenLevel = pContext.level();
        BlockPos blockPos = pContext.origin();
        Rotation rotation = Rotation.getRandom(random);
        FossilFeatureConfiguration fossilFeatureConfiguration = pContext.config();

        int randomInt = random.nextInt(fossilFeatureConfiguration.fossilStructures.size());
        StructureTemplateManager templateManager = worldGenLevel.getLevel().getServer().getStructureManager();
        StructureTemplate fossilStructures = templateManager.getOrCreate(fossilFeatureConfiguration.fossilStructures.get(randomInt));
        StructureTemplate overlayStructures = templateManager.getOrCreate(fossilFeatureConfiguration.overlayStructures.get(randomInt));
        ChunkPos chunkPos = new ChunkPos(blockPos);
        BoundingBox boundingBox = new BoundingBox(chunkPos.getMinBlockX() - 16, worldGenLevel.getMinBuildHeight(), chunkPos.getMinBlockZ() - 16, chunkPos.getMaxBlockX() + 16, worldGenLevel.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 16);
        StructurePlaceSettings placeSettings = (new StructurePlaceSettings()).setRotation(rotation).setBoundingBox(boundingBox).setRandom(random);

        Vec3i vec3i = fossilStructures.getSize(rotation);
        BlockPos blockPos1 = blockPos.offset(-vec3i.getX() / 2, 0, -vec3i.getZ() / 2);
        int y = blockPos.getY();

        int x;
        for(x = 0; x < vec3i.getX(); ++x) {
            for(int z = 0; z < vec3i.getZ(); ++z) {
                y = Math.min(y, worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, blockPos1.getX() + x, blockPos1.getZ() + z));
            }
        }

        x = Math.max(y - 15 - random.nextInt(10), worldGenLevel.getMinBuildHeight() + 10);
        BlockPos pos = fossilStructures.getZeroPositionWithTransform(blockPos1.atY(x), Mirror.NONE, rotation);
            placeSettings.clearProcessors();
            List<StructureProcessor> Processors = fossilFeatureConfiguration.fossilProcessors.value().list();
            Objects.requireNonNull(placeSettings);
            Processors.forEach(placeSettings::addProcessor);
            fossilStructures.placeInWorld(worldGenLevel, pos, pos, placeSettings, random, 4);
            placeSettings.clearProcessors();
            Processors = fossilFeatureConfiguration.overlayProcessors.value().list();
            Objects.requireNonNull(placeSettings);
            Processors.forEach(placeSettings::addProcessor);
            overlayStructures.placeInWorld(worldGenLevel, pos, pos, placeSettings, random, 4);
            return true;
    }



}
