/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.render.sky;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.mixin.minecraft.accessors.BlockBehaviourAccessor_LightPillars;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which blocks stop the light that light pillars carry from a light up to the crystals, and up to which height.
 * A block stops it up to the top of its outline shape, unless it is built without collision, like plants, fluids, and
 * torches, or it is listed in assets/frostedheart/datasets/light_pillars/passes_light.csv, like glass and clear ice.
 * The table holds block ids and tags written with a leading #. The height of each block state is cached, so clear
 * must be called when block tags change. Not thread safe.
 */
public final class LightPillarOccluders {
    /**
     * The stopped height of a block that lets light through.
     */
    public static final double PASSES = Double.NEGATIVE_INFINITY;
    private static final String FOLDER = "light_pillars";
    private static final String FILE_NAME = "passes_light.csv";
    /**
     * A light without an outline shape or a fluid counts as reaching up to the middle of its block.
     */
    private static final double SHAPELESS_LIGHT_TOP = 0.5;
    /**
     * The cached height of a state whose height depends on where it stands.
     */
    private static final float DEPENDS_ON_POSITION = Float.NaN;
    private static final float NOT_CACHED = Float.POSITIVE_INFINITY;

    @Nullable
    private static PassingBlocks passingBlocks;

    private final Reference2FloatOpenHashMap<BlockState> heights = new Reference2FloatOpenHashMap<>();

    public LightPillarOccluders() {
        heights.defaultReturnValue(NOT_CACHED);
    }

    /**
     * Whether the state lets light through at any height: air, blocks built without collision, and listed blocks.
     */
    public static boolean passesLight(BlockState state) {
        return state.isAir() || !((BlockBehaviourAccessor_LightPillars) state.getBlock()).frostedheart$hasCollision()
                || passingBlocks().contains(state);
    }

    /**
     * The height above the bottom of a light's block from which its light leaves: the top of its outline shape or of
     * its fluid, whichever is higher, or the middle of the block when it has neither. A shape that cannot be found
     * counts as none.
     */
    public static double lightTop(BlockState state, BlockGetter level, BlockPos pos) {
        double shapeTop;
        try {
            VoxelShape shape = state.getShape(level, pos);
            shapeTop = shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y);
        } catch (RuntimeException exception) {
            shapeTop = 0.0;
        }
        double top = Math.max(shapeTop, state.getFluidState().getOwnHeight());
        return top > 0.0 ? top : SHAPELESS_LIGHT_TOP;
    }

    /**
     * Whether a light fills its block, so that its light leaves from its whole top and its sides. A shape that cannot
     * be found counts as not filling it.
     */
    public static boolean isFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        try {
            return state.isCollisionShapeFullBlock(level, pos);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * The height above the bottom of the block at the position up to which the state stops light, or PASSES. A block
     * whose shape cannot be found stops light up to its top.
     */
    public double stoppedHeight(BlockState state, BlockGetter level, BlockPos pos) {
        float cached = cachedHeight(state);
        if (!Float.isNaN(cached)) {
            return cached;
        }
        try {
            VoxelShape shape = state.getShape(level, pos);
            return shape.isEmpty() ? PASSES : shape.max(Direction.Axis.Y);
        } catch (RuntimeException exception) {
            return 1.0;
        }
    }

    /**
     * Whether two states stop light up to the same height wherever they stand, so a change from one to the other
     * changes no horizon. States whose height depends on where they stand never count as the same.
     */
    public boolean stopsTheSame(BlockState oldState, BlockState newState) {
        return cachedHeight(oldState) == cachedHeight(newState);
    }

    /**
     * Forgets the cached heights, after block tags changed.
     */
    public void clear() {
        heights.clear();
    }

    /**
     * The height up to which the state stops light as cached, PASSES, or DEPENDS_ON_POSITION for a block whose shape
     * may change with its position or that cannot tell its shape without a world.
     */
    private float cachedHeight(BlockState state) {
        float height = heights.getFloat(state);
        if (height == NOT_CACHED) {
            height = findHeight(state);
            heights.put(state, height);
        }
        return height;
    }

    private static float findHeight(BlockState state) {
        if (passesLight(state)) {
            return (float) PASSES;
        }
        if (state.getBlock().hasDynamicShape()) {
            return DEPENDS_ON_POSITION;
        }
        try {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return shape.isEmpty() ? (float) PASSES : (float) shape.max(Direction.Axis.Y);
        } catch (RuntimeException exception) {
            return DEPENDS_ON_POSITION;
        }
    }

    private static PassingBlocks passingBlocks() {
        if (passingBlocks == null) {
            passingBlocks = PassingBlocks.load();
        }
        return passingBlocks;
    }

    /**
     * The blocks of the table, by id and by tag.
     */
    private record PassingBlocks(Set<ResourceLocation> ids, List<TagKey<Block>> tags) {
        boolean contains(BlockState state) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (id != null && ids.contains(id)) {
                return true;
            }
            for (int tag = 0; tag < tags.size(); tag++) {
                if (state.is(tags.get(tag))) {
                    return true;
                }
            }
            return false;
        }

        static PassingBlocks load() {
            Path path = ModList.get().getModFileById(FHMain.MODID).getFile()
                    .findResource("assets", FHMain.MODID, "datasets", FOLDER, FILE_NAME);
            String text;
            try {
                text = Files.readString(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read the light pillar dataset " + FILE_NAME, exception);
            }
            Set<ResourceLocation> ids = new HashSet<>();
            List<TagKey<Block>> tags = new ArrayList<>();
            String[] lines = text.split("\\R");
            for (int line = 1; line < lines.length; line++) {
                String target = lines[line].trim();
                if (target.isEmpty()) {
                    continue;
                }
                if (target.startsWith("#")) {
                    tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(target.substring(1))));
                } else {
                    ids.add(new ResourceLocation(target));
                }
            }
            return new PassingBlocks(ids, tags);
        }
    }
}
