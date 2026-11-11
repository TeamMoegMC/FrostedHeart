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

package com.teammoeg.frostedheart.content.town.ui.view;

import java.util.List;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 布告板界面共用的显示内容：页头、关键数字、条件标记、进度、便签、居民照片与属性条、物品行。
 * 各建筑的 presenter 在通用代码里生成这些记录，只含整数、等级和文字；界面只负责画。
 * <p>
 * Display content shared by the notice-board screens: header, key figures, condition chips, progress lines, notes,
 * resident photos with attribute bars, and item lines. Each building's presenter builds these records in common
 * code, holding whole numbers, levels and text only; screens just draw them.
 */
public final class BoardView {
    private BoardView() {
    }

    /** Overall state of a building, shown as the header's ink stamp. */
    public enum Status {
        RUNNING, LIMITED, STOPPED
    }

    /** How a piece of text or a bar should read. */
    public enum Tone {
        NORMAL, GOOD, WARN, BAD, MUTED
    }

    /**
     * @param frost 霜冻等级 0–3 / frost level 0 to 3
     */
    public record Header(Component name, Component subtitle, Status status, List<Component> reasons, int frost) {
    }

    /**
     * 一张关键数字纸片。{@code link} 是点击后打开的页序号，-1 表示不能点。
     * A key-figure note; {@code link} is the tab it opens when clicked, -1 for none.
     */
    public record Stat(Component title, Component value, Component sub, Tone subTone, List<Component> tooltip, int link) {
        public Stat(Component title, Component value, Component sub, Tone subTone, List<Component> tooltip) {
            this(title, value, sub, subTone, tooltip, -1);
        }
    }

    /** A condition chip; {@code stars} is a 0–5 rating drawn after the text, or -1 for none. */
    public record Chip(Component text, Tone tone, int stars, List<Component> tooltip) {
        public Chip(Component text, Tone tone, int stars) {
            this(text, tone, stars, List.of());
        }

        public static Chip of(Component text, Tone tone) {
            return new Chip(text, tone, -1, List.of());
        }
    }

    public record ProgressLine(Component title, int percent, Tone tone, boolean stopped) {
    }

    public enum NoteKind {
        IMPROVE, TALK
    }

    /**
     * 便签。{@code problem} 是问题种类，用来判断是否是上次打开后新出现的；留言为空。
     * A note. {@code problem} names the kind of problem, used to tell whether it is new since the last visit; empty for
     * small talk.
     */
    public record Note(NoteKind kind, String problem, Component line, Component action, Component signature) {
        /** Faults that stop the building; their notes say 得修 rather than 可以改进. */
        private static final java.util.Set<String> BLOCKING = java.util.Set.of("structure", "cold", "hot", "dark", "missing");

        public boolean blocking() {
            return kind == NoteKind.IMPROVE && BLOCKING.contains(problem);
        }
    }

    /** On duty at work; resting while ill; off when unable to work for another reason; free when fine with no work. */
    public enum Duty {
        ON_DUTY, RESTING, OFF, FREE
    }

    /** An attribute bar; {@code groupStart} draws a small gap before it, to separate body from nutrition. */
    public record Bar(Component label, int value, boolean main, Tone tone, boolean groupStart) {
        public Bar(Component label, int value, boolean main, Tone tone) {
            this(label, value, main, tone, false);
        }
    }

    /**
     * 一位居民的照片与人事卡内容。{@code badge} 是教育或职业，{@code efficiency} 为 -1 时不显示效率。
     * A resident's photo and personnel card; {@code badge} is the education or job, and an {@code efficiency} of -1 is
     * not shown.
     *
     * @param frost 住处的霜冻等级，画在照片上 / frost level of the resident's home, drawn on the photo
     * @param age   年龄组，决定照片上画成孩子、大人还是老人 / age group, which the portrait follows
     */
    public record Person(UUID id, String name, String initials, int color, Component badge, int efficiency, Duty duty,
                         Component reason, List<Bar> bars, List<Component> lines, int frost, int age) {
    }

    /** One item with a label and an amount, for loot, ore, meals and stock. */
    public record ItemLine(ItemStack icon, Component label, Component amount, Tone tone, List<Component> tooltip) {
        public ItemLine(ItemStack icon, Component label, Component amount, Tone tone) {
            this(icon, label, amount, tone, List.of());
        }
    }
}
