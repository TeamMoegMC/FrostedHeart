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

package com.teammoeg.frostedheart.content.town.buildings.mine.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.content.town.ui.view.WorkPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Forecast;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Output;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Site;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/**
 * 采矿基地的玩家视图：矿工、人手、明天能挖多少、连着几座矿井、矿藏还够挖几天。纯函数，只出整数。
 * <p>
 * The mine base's player view: miners, hands, tomorrow's yield, its mines and how long their deposits last. Pure;
 * text holds whole numbers only.
 */
public final class MineBaseViewPresenter {
    public static final String KEY = "gui.frostedheart.board.mine_base.";
    public static final int TAB_CREW = 1, TAB_OUTPUT = 2, TAB_MINES = 3;
    /** A deposit below this share of its full size is running out. */
    private static final int LOW_DEPOSIT = 20;
    private static final int MAX_IMPROVEMENTS = 3, MAX_NOTES = 6;

    public record Inputs(@Nullable MineBaseBuilding building, BlockPos pos, List<Resident> residents, @Nullable TeamTown town,
                         String townName, long townDay) {
    }

    private MineBaseViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static WorkView.Board present(Inputs in) {
        MineBaseBuilding building = in.building();
        List<Component> tips = List.of(t("tip.crew"), t("tip.radius", FHConfig.SERVER.TOWN.MINING.connectionRadiusBlocks.get()), t("tip.deposit"),
                t("tip.storage"));
        if (building == null) {
            Header header = new Header(Component.translatable("gui.frostedheart.town_manager.building.MineBaseBuilding"), Component.empty(),
                    Status.STOPPED, List.of(b("reason.missing")), 0);
            Output output = new Output(t("output.yield.none"), List.of(), b("reason.missing"), Component.empty(), Tone.MUTED, List.of(), tips);
            return new WorkView.Board(header, List.of(), List.of(), List.of(), List.of(), WorkPresenters.crew(List.of(), 0, t("noun")),
                    output, t("mines.title", 0), List.of(), b("reason.missing"));
        }
        FHConfig.Server.Town.Mining config = FHConfig.SERVER.TOWN.MINING;
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();
        String main = WorkPresenters.mainAttribute(config.healthWeight.get(), config.mentalWeight.get(), config.strengthWeight.get(),
                config.intelligenceWeight.get());
        List<Person> people = new ArrayList<>();
        for (Resident resident : in.residents())
            people.add(WorkPresenters.worker(resident, building, MineBaseBuilding.class, t("person.skill"), main, config.physicalActivity.get(),
                    config.learningActivity.get(), List.of(), buildings));
        double hands = WorkPresenters.hands(building, in.residents());
        MineBaseBuilding.MiningForecast forecast = in.town() == null ? null : building.getForecast(in.town());
        MineBaseBuilding.MiningDailyReport report = building.getDailyReport();
        int radius = building.getConnectionRadius();

        // ---- the mines and what is left in their ground
        List<Site> sites = new ArrayList<>();
        int usable = 0;
        double remainingTotal = 0;
        Set<ChunkPos> counted = new HashSet<>();
        double full = TerrainResourceType.ORE.getResourcePerSq();
        List<BlockPos> linked = building.getLinkedMines().stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        for (BlockPos minePos : linked) {
            AbstractTownBuilding value = buildings.get(minePos);
            ItemStack icon = new ItemStack(FHBlocks.MINE.get());
            if (!(value instanceof MineBuilding mine)) {
                sites.add(new Site(icon, t("mines.missing.name"), Component.empty(), t("mines.missing"), Tone.BAD, -1));
                continue;
            }
            ChunkPos chunk = new ChunkPos(minePos);
            double remaining = in.town() == null ? full : in.town().getRemainingTerrainResource(TerrainResourceType.ORE, chunk);
            int percent = full <= 0 ? 0 : TownFormat.percent(remaining / full);
            if (counted.add(chunk)) remainingTotal += Math.max(0, remaining);
            Component biome = Component.translatable(Util.makeDescriptionId("biome", mine.getBiomePath()));
            Component state;
            Tone tone;
            if (!mine.isBuildingWorkable()) {
                state = t("mines.unusable");
                tone = Tone.BAD;
            } else if (remaining <= 1e-6) {
                state = t("mines.empty");
                tone = Tone.BAD;
            } else {
                usable++;
                state = percent < LOW_DEPOSIT ? t("mines.low") : t("mines.ok");
                tone = percent < LOW_DEPOSIT ? Tone.WARN : Tone.GOOD;
            }
            sites.add(new Site(icon, TownBuildingLabels.name(buildings, minePos, mine), biome, state, tone, percent));
        }

        // ---- status: mine bases have no size minimum, so only the checks every room shares apply
        List<Component> room = BoardPresenters.roomReasons(building, 1, 1, 1, 1);
        List<Component> stopped = new ArrayList<>(room);
        TownProductionStopReason reason = forecast == null ? TownProductionStopReason.NONE : forecast.stopReason();
        if (stopped.isEmpty() && reason == TownProductionStopReason.NO_USABLE_MINES) stopped.add(t("reason.no_mines"));
        if (stopped.isEmpty() && reason == TownProductionStopReason.NO_ELIGIBLE_WORKERS) stopped.add(b("reason.no_workers"));
        List<Component> limited = new ArrayList<>();
        if (reason == TownProductionStopReason.TERRAIN_DEPLETED) limited.add(t("reason.depleted"));
        long lost = report.hasData() ? WorkPresenters.lost(report.items()) : 0;
        if (lost > 0) limited.add(b("work.reason.lost"));
        if (in.residents().size() < building.getMaxResidents()) limited.add(b("reason.empty_slots", building.getMaxResidents() - in.residents().size()));
        Header header = BoardPresenters.header(building, in.town(), in.pos(), in.townName(), in.townDay(), stopped, limited, 0);

        // ---- key figures
        Stat crew = BoardPresenters.workersStat(t("stat.crew"), people, building.getMaxResidents());
        Stat handsStat = new Stat(t("stat.hands"), TownFormat.hands(hands), b("work.stat.hands.sub"), Tone.MUTED,
                List.of(b("work.stat.hands.tooltip")), TAB_CREW);
        long tomorrow = forecast == null ? 0 : TownFormat.whole(Math.min(forecast.requested(), forecast.extractable()));
        Component tomorrowSub = reason == TownProductionStopReason.NONE ? t("stat.tomorrow.ok") : WorkPresenters.stopReason(reason);
        Stat tomorrowStat = new Stat(t("stat.tomorrow"), tomorrow > 0 ? t("stat.tomorrow.value", tomorrow) : b("work.stat.none"), tomorrowSub,
                reason == TownProductionStopReason.NONE ? Tone.MUTED : reason == TownProductionStopReason.TERRAIN_DEPLETED ? Tone.WARN : Tone.BAD,
                List.of(t("stat.tomorrow.tooltip")), TAB_OUTPUT);
        Stat minesStat = new Stat(t("stat.mines"), linked.isEmpty() ? b("work.stat.none") : Component.literal(usable + " / " + linked.size()),
                linked.isEmpty() ? t("stat.mines.sub.none") : t("stat.mines.sub"),
                usable > 0 ? Tone.MUTED : Tone.BAD, List.of(t("stat.mines.tooltip", radius)), TAB_MINES);

        List<Chip> chips = new ArrayList<>();
        chips.add(room.isEmpty() ? Chip.of(b("work.chip.room.ok"), Tone.GOOD) : Chip.of(b("work.chip.room.bad"), Tone.BAD));
        chips.add(Chip.of(t("chip.mines", usable), usable > 0 ? Tone.GOOD : Tone.BAD));
        int days = forecast == null || forecast.requested() <= 1e-6 ? -1 : TownFormat.days(remainingTotal, forecast.requested());
        if (days >= 0) chips.add(Chip.of(t("chip.deposit_days", days), days < 7 ? Tone.WARN : Tone.NORMAL));
        chips.add(Chip.of(t("chip.radius", radius), Tone.NORMAL));

        // ---- notes
        Component signer = BoardPresenters.signer(people, null);
        List<Note> notes = new ArrayList<>();
        if (!room.isEmpty()) notes.add(BoardPresenters.improve("structure", room.get(0), BoardPresenters.roomAction(building, 1, 1, 1), signer));
        if (reason == TownProductionStopReason.NO_USABLE_MINES)
            notes.add(linked.isEmpty()
                    ? BoardPresenters.improve("no_mines", t("note.no_mines", radius), t("note.no_mines.action", radius), signer)
                    : BoardPresenters.improve("mines_unusable", t("note.mines_unusable"), t("note.mines_unusable.action"), signer));
        if (reason == TownProductionStopReason.TERRAIN_DEPLETED)
            notes.add(BoardPresenters.improve("depleted", t("note.depleted"), t("note.depleted.action", radius), signer));
        WorkPresenters.crewNotes(notes, in.residents(), people, buildings, t("noun"), signer);
        Note lostNote = WorkPresenters.lostNote(lost, signer);
        if (lostNote != null) notes.add(lostNote);
        notes = BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);

        List<Note> talk = new ArrayList<>();
        if (!people.isEmpty()) for (int i = 1; i <= 4; i++) talk.add(BoardPresenters.chat(t("talk." + i), signer));
        if (report.hasData() && report.extracted() > 1e-6 && !people.isEmpty()) talk.add(BoardPresenters.chat(t("talk.good_day"), signer));
        int spare = Math.max(0, MAX_NOTES - notes.size());
        if (talk.size() > spare) talk = List.copyOf(talk.subList(0, spare));

        // ---- output
        Component yieldEmpty = !report.hasData() ? b("work.yield.no_report")
                : report.stopReason() != TownProductionStopReason.NONE ? b("work.yield.stopped", WorkPresenters.stopReason(report.stopReason()))
                : b("work.yield.nothing");
        Component yieldNote = lost > 0 ? b("work.yield.lost", lost)
                : report.hasData() && report.extracted() > 1e-6 ? t("output.total", TownFormat.whole(report.extracted())) : Component.empty();
        List<Forecast> forecasts = new ArrayList<>();
        forecasts.add(new Forecast(t("forecast.tomorrow"), tomorrow > 0 ? t("stat.tomorrow.value", tomorrow) : b("work.stat.none"), tomorrowSub,
                tomorrowStat.subTone(), List.of(t("stat.tomorrow.tooltip"))));
        forecasts.add(new Forecast(t("forecast.deposit"), days < 0 ? b("work.stat.none") : t("forecast.deposit.value", days),
                days < 0 ? t("forecast.deposit.unknown") : t("forecast.deposit.line"), days >= 0 && days < 7 ? Tone.WARN : Tone.MUTED,
                List.of(t("forecast.deposit.tooltip"))));
        Output output = new Output(report.hasData() ? t("output.yield") : t("output.yield.none"), WorkPresenters.yield(report.items()), yieldEmpty,
                yieldNote, lost > 0 ? Tone.WARN : Tone.MUTED, forecasts, tips);
        return new WorkView.Board(header, List.of(crew, handsStat, tomorrowStat, minesStat), chips, notes, talk,
                WorkPresenters.crew(people, building.getMaxResidents(), t("noun")), output, t("mines.title", radius), sites, t("mines.none", radius));
    }
}
