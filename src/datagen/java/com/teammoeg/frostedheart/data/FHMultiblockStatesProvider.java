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

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.teammoeg.frostedheart.content.FHBlocks;
import com.teammoeg.frostedheart.content.FHMultiblocks;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.Property;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static blusunrize.immersiveengineering.ImmersiveEngineering.rl;

public class FHMultiblockStatesProvider extends FHExtendedStatesProvider {

    private static final List<Vector3i> CUBE_THREE = BlockPos.getAllInBox(-1, -1, -1, 1, 1, 1)
            .map(BlockPos::toImmutable)
            .collect(Collectors.toList());
    private static final List<Vector3i> CUBE_TWO = BlockPos.getAllInBox(0, 0, -1, 1, 1, 0)
            .map(BlockPos::toImmutable)
            .collect(Collectors.toList());

    public FHMultiblockStatesProvider(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, exFileHelper);
    }

    protected void registerStatesAndModels() {
        createMultiblock(FHBlocks.Multi.generator, split(obj("block/multiblocks/generator.obj"), FHMultiblocks.GENERATOR));
        createMultiblock(FHBlocks.Multi.generator, split(obj("block/multiblocks/crucible.obj"), FHMultiblocks.CRUCIBLE));
        createMultiblock(FHBlocks.Multi.generator, split(obj("block/multiblocks/steam_turbine.obj"), FHMultiblocks.STEAMTURBINE));
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "FH Multiblock models/block states";
    }

    private ModelFile cubeTwo(String name, ResourceLocation top, ResourceLocation bottom,
                              ResourceLocation side, ResourceLocation front)
    {
        ModelFile baseModel = obj(name, rl("block/stone_multiblocks/cube_two.obj"),
                ImmutableMap.<String, ResourceLocation>builder()
                        .put("side", side)
                        .put("top", top)
                        .put("bottom", bottom)
                        .put("front", front)
                        .build()
        );
        return splitModel(name+"_split", baseModel, CUBE_TWO, false);
    }

    private ModelFile cubeThree(String name, ResourceLocation def, ResourceLocation front)
    {
        ModelFile baseModel = obj(name, rl("block/stone_multiblocks/cube_three.obj"),
                ImmutableMap.of("side", def, "front", front));
        return splitModel(name+"_split", baseModel, CUBE_THREE, false);
    }

    private void createMultiblock(Block b, ModelFile masterModel, ModelFile mirroredModel)
    {
        createMultiblock(b, masterModel, mirroredModel, IEProperties.FACING_HORIZONTAL, IEProperties.MIRRORED);
    }

    private void createMultiblock(Block b, ModelFile masterModel)
    {
        createMultiblock(b, masterModel, null, IEProperties.FACING_HORIZONTAL, null);
    }

    private void createMultiblock(Block b, ModelFile masterModel, @Nullable ModelFile mirroredModel,
                                  @Nullable Property<Boolean> mirroredState)
    {
        createMultiblock(b, masterModel, mirroredModel, IEProperties.FACING_HORIZONTAL, mirroredState);
    }

    private void createMultiblock(Block b, ModelFile masterModel, @Nullable ModelFile mirroredModel,
                                  EnumProperty<Direction> facing, @Nullable Property<Boolean> mirroredState)
    {
        Preconditions.checkArgument((mirroredModel==null)==(mirroredState==null));
        VariantBlockStateBuilder builder = getVariantBuilder(b);
        boolean[] possibleMirrorStates;
        if(mirroredState!=null)
            possibleMirrorStates = new boolean[]{false, true};
        else
            possibleMirrorStates = new boolean[1];
        for(boolean mirrored : possibleMirrorStates)
            for(Direction dir : facing.getAllowedValues())
            {
                final int angleY;
                final int angleX;
                if(facing.getAllowedValues().contains(Direction.UP))
                {
                    angleX = -90*dir.getYOffset();
                    if(dir.getAxis()!= Direction.Axis.Y)
                        angleY = getAngle(dir, 180);
                    else
                        angleY = 0;
                }
                else
                {
                    angleY = getAngle(dir, 180);
                    angleX = 0;
                }
                ModelFile model = mirrored?mirroredModel: masterModel;
                VariantBlockStateBuilder.PartialBlockstate partialState = builder.partialState()
                        .with(facing, dir);
                if(mirroredState!=null)
                    partialState = partialState.with(mirroredState, mirrored);
                partialState.setModels(new ConfiguredModel(model, angleX, angleY, true));
            }
    }

    private ModelFile split(ModelFile loc, TemplateMultiblock mb)
    {
        return split(loc, mb, false);
    }

    private ModelFile split(ModelFile loc, TemplateMultiblock mb, boolean mirror)
    {
        return split(loc, mb, mirror, false);
    }

    private ModelFile splitDynamic(ModelFile loc, TemplateMultiblock mb, boolean mirror)
    {
        return split(loc, mb, mirror, true);
    }

    private ModelFile split(ModelFile loc, TemplateMultiblock mb, boolean mirror, boolean dynamic)
    {
        UnaryOperator<BlockPos> transform = UnaryOperator.identity();
        if(mirror)
        {
            Vector3i size = mb.getSize(null);
            transform = p -> new BlockPos(size.getX()-p.getX()-1, p.getY(), p.getZ());
        }
        return split(loc, mb, transform, dynamic);
    }

    private ModelFile split(
            ModelFile name, TemplateMultiblock multiblock, UnaryOperator<BlockPos> transform, boolean dynamic
    )
    {
        final Vector3i offset = multiblock.getMasterFromOriginOffset();
        Stream<Vector3i> partsStream = multiblock.getStructure(null)
                .stream()
                .filter(info -> !info.state.isAir())
                .map(info -> info.pos)
                .map(transform)
                .map(p -> p.subtract(offset));
        return split(name, partsStream.collect(Collectors.toList()), dynamic);
    }

}
