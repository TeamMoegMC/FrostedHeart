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

import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 有工人的生产建筑（采矿基地、狩猎基地、货运站）共用的显示内容：概览、工人、产出，以及采矿基地的矿井。
 * <p>
 * Display content shared by the staffed production buildings (mine base, hunting base, transport station): the
 * overview, the crew, the output, and for the mine base its mines.
 */
public final class WorkView {
    private WorkView() {
    }

    /** The crew tab: who works here and what the empty posts mean. */
    public record Crew(Component headline, List<Person> people, Component slots, int emptySlots, Component empty) {
    }

    /** One figure on the output tab's forecast card, such as tomorrow's yield. */
    public record Forecast(Component title, Component value, Component line, Tone tone, List<Component> tooltip) {
    }

    /**
     * 昨天的收成和明天的预计。
     * Yesterday's yield and tomorrow's forecast.
     *
     * @param yieldNote 收成下的一句话，比如有多少没地方放 / a line under the yield, e.g. how much found no room
     */
    public record Output(Component yieldTitle, List<ItemLine> yield, Component yieldEmpty, Component yieldNote, Tone yieldTone,
                         List<Forecast> forecast, List<Component> tips) {
    }

    /**
     * 一处连着的场地，比如采矿基地的一座矿井。
     * A linked site, such as one mine of a mine base.
     *
     * @param percent 剩余储量的百分比，-1 表示不画 / remaining reserve in percent, -1 for none
     */
    public record Site(ItemStack icon, Component name, Component detail, Component state, Tone tone, int percent) {
    }

    public record Board(Header header, List<Stat> stats, List<Chip> conditions, List<Note> notes, List<Note> talk, Crew crew,
                        Output output, Component sitesTitle, List<Site> sites, Component sitesEmpty) {
    }
}
