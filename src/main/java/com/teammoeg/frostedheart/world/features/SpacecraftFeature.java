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

package com.teammoeg.frostedheart.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SpacecraftFeature extends Feature<NoneFeatureConfiguration> {
    public SpacecraftFeature(Codec<NoneFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource rand = level.getRandom();
        BlockPos start = new BlockPos(pos.getX() - 9, pos.getY() - 1 /*generator.getNoiseHeightMinusOne(pos.getX(),pos.getZ(), Heightmap.Type.WORLD_SURFACE_WG)*/, pos.getZ() - 7);
        Rotation rot = Rotation.getRandom(rand);
        StructurePlaceSettings settings = (new StructurePlaceSettings()).setRotationPivot(new BlockPos(/*9*/9, 2, 7/*8*/)).setRotation(rot).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        settings.keepLiquids = false;
        StructureTemplate template = level.getLevel().getStructureManager().get(new ResourceLocation(FHMain.MODID, "relic/spacecraft")).orElse(null);
        if (template == null) {
            return false;
        }
        BoundingBox boundingBox = template.getBoundingBox(settings, start);
        Vec3i vector3i = boundingBox.getCenter();
        return template.placeInWorld(level, start, new BlockPos(vector3i.getX(), vector3i.getY(), vector3i.getZ()), settings, level.getRandom(), 2);
    }
}
