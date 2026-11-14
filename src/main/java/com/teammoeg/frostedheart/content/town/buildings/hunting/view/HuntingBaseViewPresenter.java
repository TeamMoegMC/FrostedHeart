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

package com.teammoeg.frostedheart.content.town.buildings.hunting.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
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
 * 狩猎基地的玩家视图：猎人、人手、明天出猎几次、屋里冷不冷；昨天打到了什么。纯函数，只出整数。
 * <p>
 * The hunting base's player view: hunters, hands, how many hunts tomorrow, how warm the lodge is; what yesterday
 * brought in. Pure; text holds whole numbers only.
 */
public final class HuntingBaseViewPresenter {
    public static final String KEY = "gui.frostedheart.board.hunting_base.";
    public static final int TAB_CREW = 1, TAB_OUTPUT = 2;
    private static final int MAX_IMPROVEMENTS = 3, MAX_NOTES = 6;

    public record Inputs(@Nullable HuntingBaseBuilding building, BlockPos pos, List<Resident> residents, @Nullable TeamTown town,
                         String townName, long townDay) {
    }

    private HuntingBaseViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static WorkView.Board present(Inputs in) {
        HuntingBaseBuilding building = in.building();
        FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
        double minimum = config.minimumWorkingTemperatureCelsius.get();
        List<Component> tips = List.of(t("tip.crew"), t("tip.warm", (int) Math.ceil(minimum)), t("tip.game"), t("tip.storage"));
        if (building == null) {
            Header header = new Header(Component.translatable("gui.frostedheart.town_manager.building.HuntingBaseBuilding"), Component.empty(),
                    Status.STOPPED, List.of(b("reason.missing")), 0);
            Output output = new Output(t("output.yield.none"), List.of(), b("reason.missing"), Component.empty(), Tone.MUTED, List.of(), tips);
            return new WorkView.Board(header, List.of(), List.of(), List.of(), List.of(), WorkPresenters.crew(List.of(), 0, t("noun")),
                    output, Component.empty(), List.of(), Component.empty());
        }
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();
        String main = WorkPresenters.mainAttribute(config.healthWeight.get(), config.mentalWeight.get(), config.strengthWeight.get(),
                config.intelligenceWeight.get());
        List<Person> people = new ArrayList<>();
        for (Resident resident : in.residents())
            people.add(WorkPresenters.worker(resident, building, HuntingBaseBuilding.class, t("person.skill"), main, config.physicalActivity.get(),
                    config.learningActivity.get(), List.of(), buildings));
        double hands = WorkPresenters.hands(building, in.residents());
        HuntingBaseBuilding.HuntingForecast forecast = in.town() == null ? null : building.getForecast(in.town());
        HuntingBaseBuilding.HuntingDailyReport report = building.getDailyReport();
        double temperature = building.getEffectiveTemperature();
        TownFormat.Temperature shown = TownFormat.temperature(temperature, minimum, BoardPresenters.COOL_MARGIN);
        int frost = TownFormat.frost(temperature, minimum);
        int minArea = config.minimumFloorAreaBlocks.get(), minVolume = config.minimumInteriorVolumeBlocks.get();

        // ---- status
        List<Component> room = BoardPresenters.roomReasons(building, building.getArea(), minArea, building.getVolume(), minVolume);
        List<Component> stopped = new ArrayList<>(room);
        if (temperature < minimum) stopped.add(b("reason.cold", (int) Math.ceil(minimum)));
        TownProductionStopReason reason = forecast == null ? TownProductionStopReason.NONE : forecast.stopReason();
        if (stopped.isEmpty() && reason == TownProductionStopReason.NO_ELIGIBLE_WORKERS) stopped.add(b("reason.no_workers"));
        if (stopped.isEmpty() && reason == TownProductionStopReason.MISSING_LOOT_TABLE) stopped.add(t("reason.no_loot"));
        List<Component> limited = new ArrayList<>();
        if (shown.warmth() == TownFormat.Warmth.COOL) limited.add(b("reason.cool"));
        if (reason == TownProductionStopReason.ACCUMULATING) limited.add(t("reason.accumulating"));
        if (reason == TownProductionStopReason.TERRAIN_DEPLETED) limited.add(t("reason.depleted"));
        long lost = report.hasData() ? WorkPresenters.lost(report.items()) : 0;
        if (lost > 0) limited.add(b("work.reason.lost"));
        if (in.residents().size() < building.getMaxResidents()) limited.add(b("reason.empty_slots", building.getMaxResidents() - in.residents().size()));
        Header header = BoardPresenters.header(building, in.town(), in.pos(), in.townName(), in.townDay(), stopped, limited, frost);

        // ---- key figures
        Stat crew = BoardPresenters.workersStat(t("stat.crew"), people, building.getMaxResidents());
        Stat handsStat = new Stat(t("stat.hands"), TownFormat.hands(hands), b("work.stat.hands.sub"), Tone.MUTED,
                List.of(b("work.stat.hands.tooltip")), TAB_CREW);
        // A stopped base does not go out tomorrow, whatever the forecast would say for a working one.
        boolean stoppedNow = !building.isBuildingWorkable();
        int hunts = stoppedNow || forecast == null ? 0 : Math.min(forecast.plannedRolls(), forecast.availableRolls());
        Component tomorrowSub = stoppedNow ? t("stat.tomorrow.stopped") : switch (reason) {
            case NONE -> t("stat.tomorrow.ok");
            case ACCUMULATING -> t("stat.tomorrow.saving");
            default -> WorkPresenters.stopReason(reason);
        };
        Tone tomorrowTone = stoppedNow ? Tone.BAD : reason == TownProductionStopReason.NONE ? Tone.MUTED
                : reason == TownProductionStopReason.ACCUMULATING || reason == TownProductionStopReason.TERRAIN_DEPLETED ? Tone.WARN : Tone.BAD;
        Stat tomorrowStat = new Stat(t("stat.tomorrow"), hunts > 0 ? t("stat.tomorrow.value", hunts) : b("work.stat.none"), tomorrowSub,
                tomorrowTone, List.of(t("stat.tomorrow.tooltip")), TAB_OUTPUT);
        Stat warmth = new Stat(t("stat.temperature"), shown.text(), shown.warmth().text(),
                shown.warmth() == TownFormat.Warmth.WARM ? Tone.GOOD : shown.warmth() == TownFormat.Warmth.COOL ? Tone.WARN : Tone.BAD,
                List.of(t("stat.temperature.tooltip", (int) Math.ceil(minimum))));

        List<Chip> chips = new ArrayList<>();
        chips.add(BoardPresenters.spaceChip(room.isEmpty(), building.getMaxResidents()));
        chips.add(BoardPresenters.temperatureChip(temperature, minimum, Double.POSITIVE_INFINITY));
        if (building.getTemperatureModifier() > 0) chips.add(Chip.of(b("work.chip.heated"), Tone.GOOD));

        // ---- notes
        Component signer = BoardPresenters.signer(people, null);
        List<Note> notes = new ArrayList<>();
        if (!room.isEmpty())
            notes.add(BoardPresenters.improve("structure", room.get(0), BoardPresenters.roomAction(building, building.getArea(), minArea, minVolume), signer));
        Note cold = BoardPresenters.coldNote(temperature, minimum, signer);
        if (cold != null) notes.add(cold);
        WorkPresenters.crewNotes(notes, in.residents(), people, buildings, t("noun"), signer);
        if (reason == TownProductionStopReason.ACCUMULATING)
            notes.add(BoardPresenters.improve("accumulating", t("note.accumulating"), t("note.accumulating.action"), signer));
        if (reason == TownProductionStopReason.TERRAIN_DEPLETED)
            notes.add(BoardPresenters.improve("depleted", t("note.depleted"), t("note.depleted.action"), signer));
        if (reason == TownProductionStopReason.MISSING_LOOT_TABLE)
            notes.add(BoardPresenters.improve("no_loot", t("note.no_loot"), t("note.no_loot.action"), signer));
        Note lostNote = WorkPresenters.lostNote(lost, signer);
        if (lostNote != null) notes.add(lostNote);
        notes = BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);

        List<Note> talk = new ArrayList<>();
        if (!people.isEmpty()) {
            if (frost == 0) talk.add(BoardPresenters.chat(t("talk.warm"), signer));
            for (int i = 1; i <= 4; i++) talk.add(BoardPresenters.chat(t("talk." + i), signer));
        }
        int spare = Math.max(0, MAX_NOTES - notes.size());
        if (talk.size() > spare) talk = List.copyOf(talk.subList(0, spare));

        // ---- output
        Component yieldEmpty = !report.hasData() ? b("work.yield.no_report")
                : report.stopReason() != TownProductionStopReason.NONE ? b("work.yield.stopped", WorkPresenters.stopReason(report.stopReason()))
                : b("work.yield.nothing");
        Component yieldNote = lost > 0 ? b("work.yield.lost", lost)
                : report.hasData() && report.executedRolls() > 0 ? t("output.total", report.executedRolls()) : Component.empty();
        List<Forecast> forecasts = new ArrayList<>();
        forecasts.add(new Forecast(t("forecast.tomorrow"), hunts > 0 ? t("stat.tomorrow.value", hunts) : b("work.stat.none"), tomorrowSub,
                tomorrowTone, List.of(t("stat.tomorrow.tooltip"))));
        forecasts.add(new Forecast(t("forecast.hands"), TownFormat.hands(hands), t("forecast.hands.line"), Tone.MUTED,
                List.of(b("work.stat.hands.tooltip"))));
        Output output = new Output(report.hasData() ? t("output.yield") : t("output.yield.none"), WorkPresenters.yield(report.items()), yieldEmpty,
                yieldNote, lost > 0 ? Tone.WARN : Tone.MUTED, forecasts, tips);
        return new WorkView.Board(header, List.of(crew, handsStat, tomorrowStat, warmth), chips, notes, talk,
                WorkPresenters.crew(people, building.getMaxResidents(), t("noun")), output, Component.empty(), List.of(), Component.empty());
    }
}
