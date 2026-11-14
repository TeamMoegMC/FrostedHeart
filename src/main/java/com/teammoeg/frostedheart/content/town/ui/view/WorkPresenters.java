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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionReportItem;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Crew;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 有工人的生产建筑共用的规则：工人卡、班组页、产出清单、停工原因和工人相关的便签。全是纯函数。
 * <p>
 * Rules shared by the staffed production buildings: worker cards, the crew tab, yield lists, stop reasons and the
 * crew notes. All pure.
 */
public final class WorkPresenters {
    private WorkPresenters() {
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    /** The attribute a job leans on most, by its four productivity weights. */
    public static String mainAttribute(double health, double mental, double strength, double intelligence) {
        double best = Math.max(Math.max(health, mental), Math.max(strength, intelligence));
        if (best == strength) return "strength";
        if (best == intelligence) return "intelligence";
        if (best == health) return "health";
        return "mental";
    }

    /** A worker's photo and personnel card: efficiency from the building's score, proficiency in this kind of work. */
    public static Person worker(Resident resident, AbstractTownResidentWorkBuilding building, Class<? extends ITownResidentWorkBuilding> type,
                                Component skill, String mainAttribute, double physical, double learning, List<Component> extraLines,
                                Map<BlockPos, AbstractTownBuilding> buildings) {
        // On duty only while the building itself works; a fit worker at a stopped building is off (歇工), as on the seal.
        return BoardPresenters.person(resident, building.isBuildingWorkable() && building.canResidentWork(resident),
                building.getResidentScore(resident), type, skill,
                mainAttribute, physical, learning, extraLines, buildings);
    }

    /** Hands: the crew's combined standard workers, counting only those who can work. */
    public static double hands(AbstractTownResidentWorkBuilding building, List<Resident> residents) {
        return residents.stream().filter(building::canResidentWork).mapToDouble(building::getResidentScore).filter(v -> v > 0).sum();
    }

    /** The crew tab of a building whose posts are filled by the town each morning. */
    public static Crew crew(List<Person> people, int slots, Component noun) {
        long onDuty = people.stream().filter(p -> p.duty() == Duty.ON_DUTY).count();
        Component slotsLine = people.size() < slots ? b("work.crew.free", slots - people.size()) : b("work.crew.full");
        return new Crew(b("work.crew.headline", noun, onDuty, slots), people, slotsLine, Math.max(0, slots - people.size()),
                b("work.crew.none", noun));
    }

    /** Why production stopped, in a player's words. */
    public static Component stopReason(TownProductionStopReason reason) {
        return b("stop." + reason.name().toLowerCase(Locale.ROOT));
    }

    /** Yesterday's yield, largest first; an item that partly found no room in storage is marked. */
    public static List<ItemLine> yield(List<TownProductionReportItem> items) {
        List<ItemLine> lines = new ArrayList<>();
        for (TownProductionReportItem item : items) {
            if (item.produced() <= 1e-9) continue;
            ItemStack stack = new ItemStack(item.item());
            Component amount = TownFormat.underOne(item.produced()) ? b("work.yield.little")
                    : Component.literal("× " + TownFormat.whole(item.produced()));
            double lost = item.produced() - item.stored();
            Tone tone = lost > 0.5 ? Tone.WARN : Tone.NORMAL;
            List<Component> tooltip = lost > 0.5
                    ? List.of(stack.getHoverName(), b("work.yield.lost.tooltip", TownFormat.hands(item.produced()), TownFormat.hands(item.stored())))
                    : List.of(stack.getHoverName());
            lines.add(new ItemLine(stack, stack.getHoverName(), amount, tone, tooltip));
        }
        return lines;
    }

    /** Whole items of yesterday's yield that found no room in storage. */
    public static long lost(List<TownProductionReportItem> items) {
        double lost = 0;
        for (TownProductionReportItem item : items) lost += Math.max(0, item.produced() - item.stored());
        return TownFormat.whole(lost);
    }

    /**
     * 工人相关的便签：没人来干活、有人干不了活。
     * Crew notes: nobody posted here, or a worker who cannot work.
     */
    public static void crewNotes(List<Note> notes, List<Resident> residents, List<Person> people, Map<BlockPos, AbstractTownBuilding> buildings,
                                 Component noun, Component signer) {
        if (residents.isEmpty()) {
            notes.add(BoardPresenters.improve("no_workers", b("work.note.no_workers", noun), b("work.note.no_workers.action"), signer));
            return;
        }
        for (int i = 0; i < people.size(); i++) {
            // Someone fit is off only because the building stopped, and the building's own note already says why.
            if (people.get(i).duty() == Duty.ON_DUTY || BoardPresenters.fitToWork(residents.get(i))) continue;
            notes.add(BoardPresenters.unfitNote(residents.get(i), people.get(i), people, buildings));
            return;
        }
    }

    /** A note when yesterday's yield partly found no room in storage; null otherwise. */
    public static Note lostNote(long lost, Component signer) {
        if (lost <= 0) return null;
        return BoardPresenters.improve("storage_full", b("work.note.lost", lost), b("work.note.lost.action"), signer);
    }
}
