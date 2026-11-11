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

package com.teammoeg.frostedheart.content.town.buildings.logistics.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.content.town.ui.view.WorkPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Forecast;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Output;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 货运站的玩家视图：搬运工、人手、明天能跑出多少运力、全镇运力用了多少。纯函数，只出整数。
 * <p>
 * The transport station's player view: carriers, hands, tomorrow's transport capacity and how much of the town's
 * capacity is in use. Pure; text holds whole numbers only.
 */
public final class TransportStationViewPresenter {
    public static final String KEY = "gui.frostedheart.board.transport.";
    public static final int TAB_CREW = 1, TAB_OUTPUT = 2;
    private static final int MAX_IMPROVEMENTS = 3, MAX_NOTES = 6;

    public record Inputs(@Nullable TransportStationBuilding building, BlockPos pos, List<Resident> residents, @Nullable TeamTown town,
                         String townName, long townDay) {
    }

    private TransportStationViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static WorkView.Board present(Inputs in) {
        TransportStationBuilding building = in.building();
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        List<Component> tips = List.of(t("tip.what"), t("tip.cost"), t("tip.crew"), t("tip.daily"));
        if (building == null) {
            Header header = new Header(Component.translatable("gui.frostedheart.town_manager.building.TransportStationBuilding"),
                    Component.empty(), Status.STOPPED, List.of(b("reason.missing")), 0);
            Output output = new Output(t("output.yield.none"), List.of(), b("reason.missing"), Component.empty(), Tone.MUTED, List.of(), tips);
            return new WorkView.Board(header, List.of(), List.of(), List.of(), List.of(), WorkPresenters.crew(List.of(), 0, t("noun")),
                    output, Component.empty(), List.of(), Component.empty());
        }
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();
        String main = WorkPresenters.mainAttribute(config.healthWeight.get(), config.mentalWeight.get(), config.strengthWeight.get(),
                config.intelligenceWeight.get());
        List<Person> people = new ArrayList<>();
        for (Resident resident : in.residents())
            people.add(WorkPresenters.worker(resident, building, TransportStationBuilding.class, t("person.skill"), main,
                    config.physicalActivity.get(), config.learningActivity.get(), List.of(), buildings));
        double hands = WorkPresenters.hands(building, in.residents());
        TransportStationBuilding.TransportStationForecast forecast = in.town() == null ? null : building.getForecast(in.town());
        TransportStationBuilding.TransportStationDailyReport report = building.getDailyReport();
        TownTransportState.DailyReport townReport = in.town() == null ? TownTransportState.DailyReport.EMPTY
                : in.town().getTransportState().getDailyReport();
        int minArea = config.minimumFloorAreaBlocks.get(), minVolume = config.minimumInteriorVolumeBlocks.get();

        // ---- status
        List<Component> room = BoardPresenters.roomReasons(building, building.getArea(), minArea, building.getVolume(), minVolume);
        List<Component> stopped = new ArrayList<>(room);
        TownProductionStopReason reason = forecast == null ? TownProductionStopReason.NONE : forecast.stopReason();
        if (stopped.isEmpty() && reason == TownProductionStopReason.NO_ELIGIBLE_WORKERS) stopped.add(b("reason.no_workers"));
        if (stopped.isEmpty() && reason == TownProductionStopReason.OUTPUT_DISABLED) stopped.add(t("reason.disabled"));
        List<Component> limited = new ArrayList<>();
        if (report.hasData() && report.stopReason() == TownProductionStopReason.RESOURCE_REJECTED) limited.add(t("reason.rejected"));
        if (in.residents().size() < building.getMaxResidents()) limited.add(b("reason.empty_slots", building.getMaxResidents() - in.residents().size()));
        Header header = BoardPresenters.header(building, in.town(), in.pos(), in.townName(), in.townDay(), stopped, limited, 0);

        // ---- key figures
        Stat crew = BoardPresenters.workersStat(t("stat.crew"), people, building.getMaxResidents());
        Stat handsStat = new Stat(t("stat.hands"), TownFormat.hands(hands), b("work.stat.hands.sub"), Tone.MUTED,
                List.of(b("work.stat.hands.tooltip")), TAB_CREW);
        long tomorrow = forecast == null ? 0 : TownFormat.whole(forecast.plannedCapacity());
        Component tomorrowSub = reason == TownProductionStopReason.NONE ? t("stat.tomorrow.ok") : WorkPresenters.stopReason(reason);
        Tone tomorrowTone = reason == TownProductionStopReason.NONE ? Tone.MUTED : Tone.BAD;
        Stat tomorrowStat = new Stat(t("stat.tomorrow"), tomorrow > 0 ? t("stat.tomorrow.value", tomorrow) : b("work.stat.none"), tomorrowSub,
                tomorrowTone, List.of(t("stat.tomorrow.tooltip")), TAB_OUTPUT);
        long townTotal = TownFormat.whole(townReport.totalCapacity()), townReserved = (long) Math.ceil(townReport.reservedCapacity() - 1e-9);
        int usedPercent = townTotal <= 0 ? 0 : TownFormat.percent(townReport.reservedCapacity(), townReport.totalCapacity());
        Stat townStat = new Stat(t("stat.town"), townReport.hasData() ? Component.literal(TownFormat.count(townTotal)) : b("work.stat.none"),
                !townReport.hasData() ? t("stat.town.none") : t("stat.town.used", TownFormat.count(townReserved), usedPercent),
                usedPercent >= 90 ? Tone.BAD : usedPercent >= 70 ? Tone.WARN : Tone.MUTED, List.of(t("stat.town.tooltip")));

        List<Chip> chips = new ArrayList<>();
        chips.add(BoardPresenters.spaceChip(room.isEmpty(), building.getMaxResidents()));
        chips.add(Chip.of(t("chip.area", building.getArea()), Tone.NORMAL));

        // ---- notes
        Component signer = BoardPresenters.signer(people, null);
        List<Note> notes = new ArrayList<>();
        if (!room.isEmpty())
            notes.add(BoardPresenters.improve("structure", room.get(0), BoardPresenters.roomAction(building, building.getArea(), minArea, minVolume), signer));
        WorkPresenters.crewNotes(notes, in.residents(), people, buildings, t("noun"), signer);
        if (townReport.hasData() && townTotal > 0 && usedPercent >= 90)
            notes.add(BoardPresenters.improve("town_full", t("note.town_full"), t("note.town_full.action"), signer));
        if (reason == TownProductionStopReason.OUTPUT_DISABLED)
            notes.add(BoardPresenters.improve("disabled", t("note.disabled"), t("note.disabled.action"), signer));
        notes = BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);

        List<Note> talk = new ArrayList<>();
        if (!people.isEmpty()) for (int i = 1; i <= 4; i++) talk.add(BoardPresenters.chat(t("talk." + i), signer));
        int spare = Math.max(0, MAX_NOTES - notes.size());
        if (talk.size() > spare) talk = List.copyOf(talk.subList(0, spare));

        // ---- output: capacity is no item, so the yield sheet carries a sentence instead of a list
        long added = TownFormat.whole(report.addedCapacity());
        Component yieldEmpty = !report.hasData() ? b("work.yield.no_report")
                : added > 0 ? t("output.added", added)
                : report.stopReason() != TownProductionStopReason.NONE ? b("work.yield.stopped", WorkPresenters.stopReason(report.stopReason()))
                : b("work.yield.nothing");
        Component yieldNote = report.hasData() && report.workerCount() > 0 ? t("output.workers", report.workerCount()) : Component.empty();
        List<Forecast> forecasts = new ArrayList<>();
        forecasts.add(new Forecast(t("forecast.tomorrow"), tomorrow > 0 ? t("stat.tomorrow.value", tomorrow) : b("work.stat.none"), tomorrowSub,
                tomorrowTone, List.of(t("stat.tomorrow.tooltip"))));
        forecasts.add(new Forecast(t("forecast.town"), townReport.hasData() ? Component.literal(TownFormat.count(townTotal)) : b("work.stat.none"),
                townReport.hasData() ? t("stat.town.used", TownFormat.count(townReserved), usedPercent) : t("stat.town.none"), townStat.subTone(),
                List.of(t("stat.town.tooltip"))));
        Output output = new Output(report.hasData() ? t("output.yield") : t("output.yield.none"), List.of(), yieldEmpty, yieldNote, Tone.MUTED,
                forecasts, tips);
        return new WorkView.Board(header, List.of(crew, handsStat, tomorrowStat, townStat), chips, notes, talk,
                WorkPresenters.crew(people, building.getMaxResidents(), t("noun")), output, Component.empty(), List.of(), Component.empty());
    }
}
