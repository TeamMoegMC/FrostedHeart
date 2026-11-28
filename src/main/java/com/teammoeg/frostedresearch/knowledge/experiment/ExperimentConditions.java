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
import net.minecraft.world.item.ItemStack;
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
        float measured = WorldTemperature.getTempAtBlock(level, table);
        boolean ok = Float.isFinite(measured) && min.map(m -> measured >= m).orElse(true) && max.map(m -> measured <= m).orElse(true);
        String range = min.isPresent() && max.isPresent() ? format(min.get()) + "–" + format(max.get())
                : min.isPresent() ? "≥ " + format(min.get()) : "≤ " + format(max.get());
        return Optional.of(new Line("space.temperature", List.of(Arg.text(Float.isFinite(measured) ? format(measured) : "?"), Arg.text(range)), ok));
    }

    /** Allocate each input once, in definition and slot order, keeping exact stacks for completion. */
    public record Materials(List<Line> checks, int[] spent, List<ItemStack> returns) {
        public void consume(IItemHandler inventory) {
            for (int slot = 0; slot < spent.length; slot++)
                if (spent[slot] > 0) inventory.extractItem(slot, spent[slot], false);
        }
    }

    public static Materials allocate(IItemHandler inventory, List<ProjectRequirement.Material> materials) {
        int[] spent = new int[inventory.getSlots()];
        List<ItemStack> returns = new ArrayList<>();
        List<Line> checks = new ArrayList<>();
        for (var material : materials) {
            int need = material.count();
            for (int slot = 0; slot < spent.length && need > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!material.matches(stack)) continue;
                int take = Math.min(need, stack.getCount() - spent[slot]);
                if (take == 0) continue;
                spent[slot] += take;
                need -= take;
                if (material.returnOnComplete()) returns.add(stack.copyWithCount(take));
            }
            checks.add(new Line(material.returnOnComplete() ? "material.returned" : "material",
                    List.of(name(material), Arg.text(material.count() - need), Arg.text(material.count())), need == 0));
        }
        return new Materials(checks, spent, returns);
    }

    public static List<Line> materials(IItemHandler inventory, List<ProjectRequirement.Material> materials) {
        return allocate(inventory, materials).checks();
    }

    public static boolean consume(IItemHandler inventory, List<ProjectRequirement.Material> materials) {
        Materials allocation = allocate(inventory, materials);
        if (!allocation.checks().stream().allMatch(Line::ok)) return false;
        allocation.consume(inventory);
        return true;
    }

    private static Arg name(ProjectRequirement.BlockCount block) {
        return block.block().map(ForgeRegistries.BLOCKS::getValue).map(b -> Arg.key(b.getDescriptionId()))
                .orElseGet(() -> Arg.text("#" + block.tag().map(Object::toString).orElse("?")));
    }

    private static Arg name(ProjectRequirement.Material material) {
        return material.item().map(ForgeRegistries.ITEMS::getValue).map(item -> Arg.key(item.getDescriptionId()))
                .orElseGet(() -> material.toolAction().map(action -> Arg.key("knowledge.frostedresearch.tool_action." + action))
                        .orElseGet(() -> Arg.text("#" + material.tag().map(Object::toString).orElse("?"))));
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
            countBlock(pos);
        }

        @Override
        protected void processNonAirBlock(BlockPos pos) {
            countBlock(pos);
        }

        private void countBlock(BlockPos pos) {
            if (wanted.isEmpty()) return;
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) return;
            for (int i = 0; i < wanted.size(); i++) if (wanted.get(i).matches(state)) counts[i]++;
        }
    }
}
