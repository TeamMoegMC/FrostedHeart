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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.ConfinedSpaceScanner;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentReport.Arg;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentReport.Line;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 实验台的条件检查：一次有上限的房间扫描、实验台处的温度和材料槽。不保存状态。
 * Experiment table checks: one bounded room scan, the temperature at the table and the material slots. Stateless.
 */
public final class ExperimentConditions {
    private static final Direction[] START = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private ExperimentConditions() {
    }

    /**
     * Space lines. The room is the air reachable from the table's first open side, upward first.
     */
    public static List<Line> space(Level level, BlockPos table, ProjectRequirement.Space space) {
        List<Line> lines = new ArrayList<>();
        if (!space.enclosed()) return lines;
        BlockPos start = null;
        for (Direction direction : START)
            if (AbstractBlockScanner.isAirOrLadder(level, table.relative(direction))) {
                start = table.relative(direction);
                break;
            }
        RoomScanner room = start == null ? null : new RoomScanner(level, start, space.blocks());
        boolean enclosed = room != null && room.scan() && room.isValid();
        lines.add(new Line(start == null ? "space.no_air" : enclosed ? "space.enclosed" : "space.open",
                List.of(Arg.text(ProjectRequirement.MAX_VOLUME)), enclosed));
        if (enclosed)
            lines.add(new Line("space.volume", List.of(Arg.text(room.volume), Arg.text(space.minVolume()), Arg.text(space.maxVolume())),
                    room.volume >= space.minVolume() && room.volume <= space.maxVolume()));
        for (int i = 0; i < space.blocks().size(); i++) {
            var block = space.blocks().get(i);
            int have = enclosed ? room.counts[i] : 0;
            lines.add(new Line("space.block", List.of(name(block), Arg.text(have), Arg.text(block.count())), enclosed && have >= block.count()));
        }
        return lines;
    }

    /**
     * The temperature line, when the space asks for a temperature range.
     */
    public static Optional<Line> temperature(Level level, BlockPos table, ProjectRequirement.Space space) {
        var min = space.minTemperature();
        var max = space.maxTemperature();
        if (min.isEmpty() && max.isEmpty()) return Optional.empty();
        float measured = WorldTemperature.block(level, table);
        boolean ok = Float.isFinite(measured) && min.map(m -> measured >= m).orElse(true) && max.map(m -> measured <= m).orElse(true);
        String range = min.isPresent() && max.isPresent() ? format(min.get()) + "–" + format(max.get())
                : min.isPresent() ? "≥ " + format(min.get()) : "≤ " + format(max.get());
        return Optional.of(new Line("space.temperature", List.of(Arg.text(Float.isFinite(measured) ? format(measured) : "?"), Arg.text(range)), ok));
    }

    /**
     * What the material slots can supply. Each stack serves one material at a time, in the order they are listed,
     * which is also the order {@link #consume} takes them in.
     */
    public static List<Line> materials(IItemHandler inventory, List<ProjectRequirement.Material> materials) {
        int[] taken = allot(inventory, materials);
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            var material = materials.get(i);
            lines.add(new Line("material", List.of(name(material), Arg.text(taken[i]), Arg.text(material.count())), taken[i] >= material.count()));
        }
        return lines;
    }

    /**
     * Takes every material out of the slots, or nothing when one falls short.
     */
    public static boolean consume(IItemHandler inventory, List<ProjectRequirement.Material> materials) {
        int[] taken = allot(inventory, materials);
        for (int i = 0; i < materials.size(); i++) if (taken[i] < materials.get(i).count()) return false;
        for (var material : materials) {
            int need = material.count();
            for (int slot = 0; slot < inventory.getSlots() && need > 0; slot++)
                if (material.matches(inventory.getStackInSlot(slot)))
                    need -= inventory.extractItem(slot, need, false).getCount();
        }
        return true;
    }

    private static int[] allot(IItemHandler inventory, List<ProjectRequirement.Material> materials) {
        int[] left = new int[inventory.getSlots()];
        for (int slot = 0; slot < left.length; slot++) left[slot] = inventory.getStackInSlot(slot).getCount();
        int[] taken = new int[materials.size()];
        for (int i = 0; i < materials.size(); i++) {
            var material = materials.get(i);
            for (int slot = 0; slot < left.length && taken[i] < material.count(); slot++)
                if (left[slot] > 0 && material.matches(inventory.getStackInSlot(slot))) {
                    int take = Math.min(left[slot], material.count() - taken[i]);
                    left[slot] -= take;
                    taken[i] += take;
                }
        }
        return taken;
    }

    private static Arg name(ProjectRequirement.BlockCount block) {
        return block.block().map(ForgeRegistries.BLOCKS::getValue).map(b -> Arg.key(b.getDescriptionId()))
                .orElseGet(() -> Arg.text("#" + block.tag().map(Object::toString).orElse("?")));
    }

    private static Arg name(ProjectRequirement.Material material) {
        return material.item().map(ForgeRegistries.ITEMS::getValue).map(item -> Arg.key(item.getDescriptionId()))
                .orElseGet(() -> Arg.text("#" + material.tag().map(Object::toString).orElse("?")));
    }

    private static String format(double celsius) {
        return String.format(Locale.ROOT, "%.1f°C", celsius);
    }

    /**
     * Counts the room's air and the blocks touching it, within {@link ProjectRequirement#MAX_VOLUME} air blocks.
     */
    private static final class RoomScanner extends ConfinedSpaceScanner {
        private final List<ProjectRequirement.BlockCount> wanted;
        private final int[] counts;
        private int volume;

        RoomScanner(Level level, BlockPos start, List<ProjectRequirement.BlockCount> wanted) {
            super(level, start, ProjectRequirement.MAX_VOLUME);
            this.wanted = wanted;
            this.counts = new int[wanted.size()];
        }

        @Override
        protected void processAirBlock(BlockPos pos) {
            volume++;
        }

        @Override
        protected void processNonAirBlock(BlockPos pos) {
            BlockState state = world.getBlockState(pos);
            for (int i = 0; i < wanted.size(); i++) if (wanted.get(i).matches(state)) counts[i]++;
        }
    }
}
