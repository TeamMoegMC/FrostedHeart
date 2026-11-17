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

package com.teammoeg.frostedheart.content.town.buildings.institute.view;

import java.util.List;

import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Chip;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Header;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Note;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Person;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.ProgressLine;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Stat;

import net.minecraft.network.chat.Component;

/**
 * 研究所玩家视图要显示的全部内容，由 {@link InstituteViewPresenter} 从建筑、居民、城镇和知识快照算出。
 * 这里只有整数、等级和文字，没有小数；界面只负责把它画出来。通用部分见 {@code BoardView}。
 * <p>
 * Everything the research institute's player view shows, computed by {@link InstituteViewPresenter} from the
 * building, its researchers, the town and the knowledge snapshot. Whole numbers, levels and text only; the shared
 * parts live in {@code BoardView}.
 */
public record InstituteView(Header header, Overview overview, List<Note> notes, List<Note> talk,
                            Researchers researchers, Research research, Association association) {

    public record Capacity(int archiveUsed, int archiveLimit, int inboxUsed, int inboxLimit, int shelves,
                           List<Component> tooltip) {
    }

    public record Overview(Stat researchers, Stat hands, Capacity capacity, Stat association, List<Chip> conditions,
                           List<ProgressLine> research) {
    }

    public record Researchers(Component headline, List<Person> people, Component slots, int emptySlots) {
    }

    public enum OrderStamp {
        NONE, SHORT, STOP, DONE
    }

    public record WorkOrder(String run, String project, Component title, boolean experiment, boolean required,
                            int percent, Component progress, List<Component> details, Component today,
                            Component problem, OrderStamp stamp, int education) {
    }

    public record Research(Component headline, List<Component> headlineTooltip, List<WorkOrder> orders,
                           Component idle, boolean idleLinksAssociation, Component empty) {
    }

    public enum Outcome {
        IDEA, HINT, QUIET, NOTHING, NONE, NEVER
    }

    /**
     * @param history 研究所想到过的想法，最新的在上 / the ideas the institute has found, newest first
     */
    public record Association(Component headline, Outcome outcome, Component title, Component detail,
                              Component action, List<Person> people, Component idle, int chanceSteps,
                              Component chance, List<Component> chanceTooltip, Component historyTitle,
                              List<PastIdea> history, List<Component> tips) {
    }

    /**
     * 研究所想到过的一个想法：名字、哪一天想到、从哪几样知识想到，以及札记里对它的描述。没有记录的部分为空。
     * <p>
     * One idea the institute found: its name, the town day, the knowledge it came from and what the journal says about
     * it. Parts without a record are empty.
     */
    public record PastIdea(com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey key, Component title, Component when,
                           Component from, Component excerpt) {
    }
}
