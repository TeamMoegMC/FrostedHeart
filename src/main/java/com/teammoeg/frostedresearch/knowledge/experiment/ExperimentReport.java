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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次实验条件检查的结果：每行一项条件，带玩家可读的参数与是否满足。
 * The outcome of one experiment check: one line per condition, with readable arguments and whether it holds.
 * <p>
 * Line keys are suffixes of {@code gui.frostedresearch.experiment.}. An argument is either literal text or a
 * translation key, such as a block or item description id.
 */
public record ExperimentReport(List<Line> lines) {
    public static final ExperimentReport EMPTY = new ExperimentReport(List.of());

    public record Arg(String value, boolean translate) {
        public static Arg text(Object value) {
            return new Arg(String.valueOf(value), false);
        }

        public static Arg key(String key) {
            return new Arg(key, true);
        }
    }

    public record Line(String key, List<Arg> args, boolean ok) {
        public Line {
            args = List.copyOf(args);
        }
    }

    public ExperimentReport {
        lines = List.copyOf(lines);
    }

    public boolean ok() {
        return lines.stream().allMatch(Line::ok);
    }

    public CompoundTag write() {
        ListTag list = new ListTag();
        for (Line line : lines) {
            CompoundTag tag = new CompoundTag();
            tag.putString("key", line.key());
            tag.putBoolean("ok", line.ok());
            ListTag args = new ListTag();
            for (Arg arg : line.args()) {
                CompoundTag value = new CompoundTag();
                value.putString("value", arg.value());
                if (arg.translate()) value.putBoolean("translate", true);
                args.add(value);
            }
            tag.put("args", args);
            list.add(tag);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("lines", list);
        return tag;
    }

    public static ExperimentReport read(CompoundTag tag) {
        List<Line> lines = new ArrayList<>();
        for (Tag raw : tag.getList("lines", Tag.TAG_COMPOUND)) {
            CompoundTag line = (CompoundTag) raw;
            List<Arg> args = new ArrayList<>();
            for (Tag value : line.getList("args", Tag.TAG_COMPOUND))
                args.add(new Arg(((CompoundTag) value).getString("value"), ((CompoundTag) value).getBoolean("translate")));
            lines.add(new Line(line.getString("key"), args, line.getBoolean("ok")));
        }
        return new ExperimentReport(lines);
    }
}
