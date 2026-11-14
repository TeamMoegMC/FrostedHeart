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

package com.teammoeg.frostedheart.item.townmanager.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import com.teammoeg.frostedheart.content.town.TownPolicyState;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.view.HuntingBaseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBuilding;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.view.TransportStationViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.view.MineBaseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.mine.view.MineViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.view.WarehouseViewPresenter;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel.Nutrient;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 镇长印章的玩家视图：全镇的状况、居民、每座建筑、近来的事和法令。各建筑的状态直接取自它自己的布告板，两边永远一致。纯函数，只出整数。
 * <p>
 * The Mayor's Seal player view: the town's state, its residents, every building, recent events and its laws. Each
 * building's status comes from that building's own board, so the two always agree. Pure; text holds whole numbers.
 */
public final class TownHallPresenter {
    public static final String KEY = "gui.frostedheart.board.town.";
    public static final String STATUS_KEY = KEY + "status.";
    public static final int TAB_RESIDENTS = 1, TAB_BUILDINGS = 2;
    private static final int MAX_IMPROVEMENTS = 4, MAX_NOTES = 7, MAX_EVENTS = 60;

    /** One building on the seal's list, worded exactly as its own board words it. */
    public record BuildingCard(ItemStack icon, Component name, Status status, Component stamp, Component line, BlockPos pos) {
    }

    public record EventLine(Component when, Component text, Tone tone) {
    }

    public record Law(TownCareLaw law, Component name, Component description, boolean active, boolean pending) {
    }

    public record Laws(Component title, List<Law> laws, Component state, Tone stateTone, boolean locked, List<Component> help) {
    }

    public record TownHallView(Header header, List<Stat> stats, List<Chip> conditions, List<Note> notes, List<Note> talk,
                               List<Chip> tower, List<Person> people, List<BuildingCard> buildings, List<EventLine> events, Laws laws) {
    }

    public record Inputs(@Nullable TeamTown town, @Nullable TeamTownData data, @Nullable TownOperationalStatus status, long day) {
    }

    private TownHallPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static TownHallView present(Inputs in) {
        TeamTown town = in.town();
        if (town == null || in.data() == null) {
            Header header = new Header(t("no_town"), Component.empty(), Status.STOPPED, List.of(t("no_town.hint")), 0);
            return new TownHallView(header, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    new Laws(t("laws.title"), List.of(), Component.empty(), Tone.MUTED, true, List.of()));
        }
        TownOperationalStatus status = in.status();
        Map<BlockPos, AbstractTownBuilding> buildings = town.getTownBuildings();
        List<Resident> residents = new ArrayList<>(town.getAllResidents());
        residents.sort(Comparator.comparing(Resident::toString, String.CASE_INSENSITIVE_ORDER).thenComparing(Resident::getUUID));
        List<TownHistoryEntry> history = in.data().getHistory();
        TownHistoryEntry latest = history.isEmpty() ? null : history.get(history.size() - 1);
        TownHistoryEntry previous = history.size() >= 2 ? history.get(history.size() - 2) : null;

        // ---- status: the worst active alert decides the stamp
        List<TownOperationalStatus.ActiveAlert> alerts = status == null ? List.of() : status.activeAlerts();
        TownSignalEvent.Severity worst = alerts.stream().map(TownOperationalStatus.ActiveAlert::severity).max(Comparator.naturalOrder()).orElse(null);
        List<Component> reasons = new ArrayList<>();
        for (TownOperationalStatus.ActiveAlert alert : alerts) reasons.add(alertLine(alert));
        Status stamp = worst == null || worst == TownSignalEvent.Severity.INFORMATION ? Status.RUNNING
                : worst == TownSignalEvent.Severity.WARNING ? Status.LIMITED : Status.STOPPED;
        int population = status != null ? status.population() : residents.size();
        Header header = new Header(Component.literal(town.getName()), t("subtitle", in.day(), population), stamp,
                reasons.isEmpty() ? List.of(t("status.calm")) : reasons, 0);

        // ---- key figures
        Component popSub;
        Tone popTone = Tone.MUTED;
        if (latest == null || previous == null) popSub = t("stat.population.new");
        else {
            int delta = latest.population() - previous.population();
            popSub = delta > 0 ? t("stat.population.up", delta) : delta < 0 ? t("stat.population.down", -delta) : t("stat.population.same");
            if (delta < 0) popTone = Tone.WARN;
        }
        Stat populationStat = new Stat(t("stat.population"), Component.literal(Integer.toString(population)), popSub, popTone, List.of(), TAB_RESIDENTS);
        double warning = FHConfig.SERVER.TOWN.OBSERVATION.reserveWarningDays.get(), critical = FHConfig.SERVER.TOWN.OBSERVATION.reserveCriticalDays.get();
        Stat food = reserveStat(t("stat.food"), status == null ? null : status.foodReserveDays(), warning, critical, t("stat.food.sub"),
                List.of(t("stat.food.tooltip")));
        Stat fuel = status != null && status.tower().kind() == TownOperationalStatus.TowerKind.T2
                ? new Stat(t("stat.fuel"), b("work.stat.none"), t("stat.fuel.t2"), Tone.MUTED, List.of(t("stat.fuel.tooltip")))
                : reserveStat(t("stat.fuel"), status == null ? null : status.fuelReserveDays(), warning, critical, t("stat.fuel.sub"),
                List.of(t("stat.fuel.tooltip")));
        boolean empty = population == 0;
        // Nobody eats in an empty town: say so instead of "still counting".
        if (empty) food = new Stat(t("stat.food"), b("work.stat.none"), t("stat.food.nobody"), Tone.MUTED, List.of(t("stat.food.tooltip")));
        double health = status != null ? status.averageHealth() : residents.stream().mapToDouble(Resident::getHealth).average().orElse(0);
        double mental = status != null ? status.averageMental() : residents.stream().mapToDouble(Resident::getMental).average().orElse(0);
        int healthShown = (int) Math.floor(health), mentalShown = (int) Math.floor(mental);
        Tone wellTone = Math.min(healthShown, mentalShown) < 30 ? Tone.BAD : Math.min(healthShown, mentalShown) < 50 ? Tone.WARN : Tone.GOOD;
        Stat wellbeing = empty ? new Stat(t("stat.wellbeing"), b("work.stat.none"), t("stat.wellbeing.nobody"), Tone.MUTED, List.of(t("stat.wellbeing.tooltip")), TAB_RESIDENTS)
                : new Stat(t("stat.wellbeing"), Component.literal(healthShown + " · " + mentalShown), t("stat.wellbeing.sub"), wellTone,
                status == null ? List.of(t("stat.wellbeing.tooltip"))
                        : List.of(t("stat.wellbeing.tooltip"), t("stat.wellbeing.weakest", (int) Math.floor(status.p10Health()), (int) Math.floor(status.p10Mental()))),
                TAB_RESIDENTS);

        // ---- the town's condition at a glance
        long homeless = status != null ? status.homelessCount() : residents.stream().filter(r -> r.getHousePos() == null).count();
        // The same split as the residents page and staffing, counted from today's residents, not the last settlement.
        BoardPresenters.Headcount heads = BoardPresenters.headcount(residents, buildings);
        int leaving = status == null ? 0 : status.exitRiskCount();
        int coldHouses = status == null ? 0 : status.unsafeOccupiedHouseCount();
        List<Chip> chips = new ArrayList<>();
        if (empty) chips.add(Chip.of(t("chip.empty"), Tone.BAD));
        else {
            chips.add(homeless == 0 ? Chip.of(t("chip.housed"), Tone.GOOD) : Chip.of(t("chip.homeless", homeless), Tone.BAD));
            // 人人有活 only while nobody is free or off; otherwise the free and off chips say who is not working.
            if (heads.free() > 0) chips.add(Chip.of(t("chip.idle", heads.free()), Tone.WARN));
            else if (heads.off() == 0) chips.add(Chip.of(t("chip.employed"), Tone.GOOD));
        }
        if (heads.off() > 0) chips.add(Chip.of(t("chip.off", heads.off()), Tone.WARN));
        if (heads.resting() > 0) chips.add(Chip.of(t("chip.resting", heads.resting()), Tone.BAD));
        if (leaving > 0) chips.add(Chip.of(t("chip.leaving", leaving), Tone.BAD));
        if (coldHouses > 0) chips.add(Chip.of(t("chip.cold_houses", coldHouses), Tone.BAD));
        if (status != null && status.climateLevel() > 0) chips.add(Chip.of(t("chip.climate", status.climateLevel()), Tone.WARN));

        // ---- notes: one per active alert, most severe first
        Component signer = t("signer");
        List<Note> notes = new ArrayList<>();
        if (empty) {
            // Refugees only come to a working tower, so an empty town without one is told to see to the tower first.
            TownOperationalStatus.TowerStatus towerNow = status == null ? null : status.tower();
            Component emptyAction = towerNow == null || towerNow.kind() == TownOperationalStatus.TowerKind.NONE ? t("note.empty.action.build_tower")
                    : !towerNow.enabled() ? t("note.empty.action.turn_on_tower") : t("note.empty.action");
            notes.add(BoardPresenters.improve("empty", t("note.empty"), emptyAction, signer));
        }
        alerts.stream().sorted(Comparator.comparing(TownOperationalStatus.ActiveAlert::severity).reversed())
                .forEach(alert -> notes.add(alertNote(alert, status, signer)));
        List<Note> improvements = BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);
        List<Note> talk = new ArrayList<>();
        if (!empty) {
            if (alerts.isEmpty()) talk.add(BoardPresenters.chat(t("talk.calm"), signer));
            for (int i = 1; i <= 3; i++) talk.add(BoardPresenters.chat(t("talk." + i), signer));
        }
        int spare = Math.max(0, MAX_NOTES - improvements.size());
        if (talk.size() > spare) talk = List.copyOf(talk.subList(0, spare));

        // ---- tower, people, buildings, events, laws
        List<Chip> tower = tower(status);
        List<Person> people = new ArrayList<>();
        for (Resident resident : residents) people.add(resident(resident, buildings));
        List<BuildingCard> cards = buildings(town, in, buildings, residents, status);
        List<EventLine> events = events(history, in.day());
        Laws laws = laws(town, in.day());
        return new TownHallView(header, List.of(populationStat, food, fuel, wellbeing), chips, improvements, talk, tower, people, cards, events, laws);
    }

    private static Stat reserveStat(Component title, @Nullable TownOperationalStatus.Metric metric, double warning, double critical, Component sub,
                                    List<Component> tooltip) {
        if (metric == null || !metric.available()) return new Stat(title, b("work.stat.none"), t("stat.reserve.unknown"), Tone.MUTED, tooltip);
        int days = (int) Math.floor(Math.max(0, metric.value()) + 1e-9);
        Tone tone = metric.value() < critical ? Tone.BAD : metric.value() < warning ? Tone.WARN : Tone.GOOD;
        // "Enough for the town" only while it is; below the lines the note says what is coming instead.
        Component line = tone == Tone.BAD ? t(key(sub) + ".short") : tone == Tone.WARN ? t(key(sub) + ".low") : sub;
        return new Stat(title, days < 1 ? t("stat.reserve.under_one") : t("stat.reserve.days", days), line, tone, tooltip);
    }

    /** The key behind a stat line built by {@link #t}, so its low and short variants can be named after it. */
    private static String key(Component line) {
        return line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                ? translatable.getKey().substring(KEY.length()) : "";
    }

    // ------------------------------------------------------------------ alerts

    private static Component alertLine(TownOperationalStatus.ActiveAlert alert) {
        Nutrient nutrient = nutrientOf(alert.type());
        if (nutrient != null) return t("alert.nutrition", alert.affectedCount(), BoardPresenters.nutrient(nutrient));
        return t("alert." + alert.type().name().toLowerCase(Locale.ROOT), alert.affectedCount());
    }

    private static Note alertNote(TownOperationalStatus.ActiveAlert alert, TownOperationalStatus status, Component signer) {
        Nutrient nutrient = nutrientOf(alert.type());
        String id = alert.type().name().toLowerCase(Locale.ROOT);
        Component action = nutrient != null ? t("alert.nutrition.action", BoardPresenters.nutrient(nutrient))
                : alert.type() == TownSignalEvent.Type.TOWER_SERVICE_LOST ? towerAction(status)
                : t("alert." + id + ".action");
        return BoardPresenters.improve(id, alertLine(alert), action, signer);
    }

    /** What to do about a cold tower, by what is wrong with it; only a vague check when nothing on record explains it. */
    private static Component towerAction(TownOperationalStatus status) {
        TownOperationalStatus.TowerStatus tower = status.tower();
        String key = "alert.tower_service_lost.";
        if (tower.broken()) return t(key + "repair");
        if (!tower.enabled()) return t(key + "turn_on");
        TownOperationalStatus.Metric fuel = status.fuelReserveDays();
        if (tower.kind() != TownOperationalStatus.TowerKind.T2 && fuel != null && fuel.available() && fuel.value() < 1) return t(key + "fuel");
        return t(key + "action");
    }

    @Nullable
    private static Nutrient nutrientOf(TownSignalEvent.Type type) {
        return switch (type) {
            case NUTRITION_FAT_SEVERE -> Nutrient.FAT;
            case NUTRITION_CARBOHYDRATE_SEVERE -> Nutrient.CARBOHYDRATE;
            case NUTRITION_PROTEIN_SEVERE -> Nutrient.PROTEIN;
            case NUTRITION_VEGETABLE_SEVERE -> Nutrient.VEGETABLE;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ tower

    /** The tower's lines, each with the tone its lamp is lit in: green while it heats, amber when it does not, red when broken. */
    private static List<Chip> tower(@Nullable TownOperationalStatus status) {
        if (status == null) return List.of(Chip.of(t("tower.waiting"), Tone.MUTED));
        TownOperationalStatus.TowerStatus tower = status.tower();
        if (tower.kind() == TownOperationalStatus.TowerKind.NONE) return List.of(Chip.of(t("tower.none"), Tone.BAD));
        List<Chip> lines = new ArrayList<>();
        // Overdrive only matters while it heats, so it is part of that line rather than a line of its own.
        if (!tower.enabled()) lines.add(Chip.of(t("tower.off"), Tone.WARN));
        else if (!tower.active()) lines.add(Chip.of(t("tower.idle"), Tone.WARN));
        else if (tower.overdrive()) lines.add(Chip.of(t("tower.overdrive"), Tone.WARN));
        else lines.add(Chip.of(t("tower.heating"), Tone.GOOD));
        int wear = TownFormat.percent(tower.overdriveFraction());
        if (tower.broken() || wear >= 100) lines.add(Chip.of(t("tower.broken"), Tone.BAD));
        else if (wear <= 0) lines.add(Chip.of(t("tower.intact"), Tone.MUTED));
        else lines.add(Chip.of(t(tower.overdrive() ? "tower.wearing" : "tower.mending", wear), tower.overdrive() ? Tone.WARN : Tone.MUTED));
        return lines;
    }

    // ------------------------------------------------------------------ residents

    private static Person resident(Resident resident, Map<BlockPos, AbstractTownBuilding> buildings) {
        Duty duty = BoardPresenters.homeDuty(resident, buildings);
        List<Component> lines = new ArrayList<>();
        BlockPos work = resident.getWorkPos();
        AbstractTownBuilding job = work == null ? null : buildings.get(work);
        if (job != null) lines.add(t("person.job", TownBuildingLabels.name(buildings, work, job)));
        else if (resident.getAge() < FHConfig.SERVER.TOWN.RESIDENT_RULES.minimumWorkingAge.get()) lines.add(t("person.too_young"));
        else lines.add(t("person.no_job"));
        Person base = BoardPresenters.person(resident, duty, -1, null, null, null, 0, 0, lines, buildings, true);
        List<Bar> bars = new ArrayList<>(base.bars());
        bars.addAll(BoardPresenters.nutritionBars(resident));
        Component badge = Component.translatable(Resident.ageLangKey(resident.getAge())).copy().append(b("separator"))
                .append(BoardPresenters.education(resident.getEducationLevel()));
        return new Person(base.id(), base.name(), base.initials(), base.color(), badge, -1, duty, base.reason(), bars, base.lines(), base.frost(), base.age());
    }

    // ------------------------------------------------------------------ buildings

    private static List<BuildingCard> buildings(TeamTown town, Inputs in, Map<BlockPos, AbstractTownBuilding> buildings, List<Resident> residents,
                                                @Nullable TownOperationalStatus status) {
        List<BuildingCard> cards = new ArrayList<>();
        String name = town.getName();
        long day = in.day();
        Double foodDays = status != null && status.foodReserveDays().available() ? status.foodReserveDays().value() : null;
        int population = status != null ? status.population() : residents.size();
        List<Map.Entry<BlockPos, AbstractTownBuilding>> entries = buildings.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<BlockPos, AbstractTownBuilding> e) -> order(e.getValue()))
                        .thenComparingLong(e -> e.getKey().asLong())).toList();
        for (Map.Entry<BlockPos, AbstractTownBuilding> entry : entries) {
            BlockPos pos = entry.getKey();
            AbstractTownBuilding building = entry.getValue();
            List<Resident> workers = residents.stream().filter(r -> pos.equals(r.getWorkPos())).toList();
            Header header;
            String statusKey = TownFormat.KEY + "status.";
            ItemLike icon;
            if (building instanceof HouseBuilding house) {
                header = HouseViewPresenter.present(new HouseViewPresenter.Inputs(house, pos,
                        residents.stream().filter(r -> pos.equals(r.getHousePos())).toList(), town, name, day)).header();
                statusKey = HouseViewPresenter.STATUS_KEY;
                icon = net.minecraft.world.item.Items.RED_BED;
            } else if (building instanceof WarehouseBuilding warehouse) {
                header = WarehouseViewPresenter.present(new WarehouseViewPresenter.Inputs(warehouse, pos, town, name, day, List.of(), foodDays,
                        population)).header();
                statusKey = WarehouseViewPresenter.STATUS_KEY;
                icon = net.minecraft.world.item.Items.CHEST;
            } else if (building instanceof MineBaseBuilding mineBase) {
                header = MineBaseViewPresenter.present(new MineBaseViewPresenter.Inputs(mineBase, pos, workers, town, name, day)).header();
                icon = net.minecraft.world.item.Items.IRON_ORE;
            } else if (building instanceof MineBuilding mine) {
                header = MineViewPresenter.present(new MineViewPresenter.Inputs(mine, pos, town, name, day)).header();
                icon = net.minecraft.world.item.Items.IRON_PICKAXE;
            } else if (building instanceof HuntingBaseBuilding hunting) {
                header = HuntingBaseViewPresenter.present(new HuntingBaseViewPresenter.Inputs(hunting, pos, workers, town, name, day)).header();
                icon = net.minecraft.world.item.Items.BOW;
            } else if (building instanceof TransportStationBuilding station) {
                header = TransportStationViewPresenter.present(new TransportStationViewPresenter.Inputs(station, pos, workers, town, name, day)).header();
                icon = net.minecraft.world.item.Items.MINECART;
            } else if (building instanceof ResearchInstituteBuilding institute) {
                header = InstituteViewPresenter.present(new InstituteViewPresenter.Inputs(institute, pos, workers, town, null, name, day)).header();
                icon = net.minecraft.world.item.Items.BOOK;
            } else continue;
            Component line = header.reasons().isEmpty() ? t("building.fine") : header.reasons().get(0);
            cards.add(new BuildingCard(new ItemStack(icon), TownBuildingLabels.name(buildings, pos, building), header.status(),
                    Component.translatable(statusKey + header.status().name().toLowerCase(Locale.ROOT)), line, pos));
        }
        return cards;
    }

    /** Homes first, then storage, then the workplaces, so the list reads like a town plan. */
    private static int order(AbstractTownBuilding building) {
        if (building instanceof HouseBuilding) return 0;
        if (building instanceof WarehouseBuilding) return 1;
        if (building instanceof ResearchInstituteBuilding) return 2;
        if (building instanceof HuntingBaseBuilding) return 3;
        if (building instanceof MineBaseBuilding) return 4;
        if (building instanceof MineBuilding) return 5;
        return 6;
    }

    // ------------------------------------------------------------------ events and laws

    /**
     * 大事记用城镇自己的日子，和页头、公文行、研究所的想法一样。历史记录按世界日记账，每次结算追加一条，城镇日也随之加一，
     * 所以第 i 条记录是城镇第「现在 - (条数 - 1 - i)」日；记录里的事件按它和这条记录相差的天数换算。
     * <p>
     * The events count in the town's own days, like the header, the document lines and the institute's ideas. History is
     * kept by world day with one entry per settlement, and every settlement also advances the town's day, so entry i is
     * town day {@code now - (size - 1 - i)}; an event inside it shifts by its own distance from that entry.
     */
    static long[] townDays(List<TownHistoryEntry> history, long townDay) {
        long[] days = new long[history.size()];
        for (int i = 0; i < days.length; i++) days[i] = Math.max(0, townDay - (days.length - 1 - i));
        return days;
    }

    private static List<EventLine> events(List<TownHistoryEntry> history, long townDay) {
        record Dated(TownSignalEvent event, long day) {
        }
        long[] days = townDays(history, townDay);
        List<Dated> events = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            TownHistoryEntry entry = history.get(i);
            for (TownSignalEvent event : entry.events())
                events.add(new Dated(event, Math.max(0, days[i] + (event.day() - entry.day()))));
        }
        events.sort(Comparator.comparingLong(Dated::day).reversed().thenComparing(Comparator.comparingInt((Dated d) -> d.event().hour()).reversed()));
        List<EventLine> lines = new ArrayList<>();
        for (Dated dated : events) {
            if (lines.size() >= MAX_EVENTS) break;
            TownSignalEvent event = dated.event();
            Tone tone = switch (event.severity()) {
                case INFORMATION -> Tone.GOOD;
                case WARNING -> Tone.WARN;
                case CRITICAL, IRREVERSIBLE -> Tone.BAD;
            };
            // Settlement events happen at the start of a day, so their hour says nothing.
            Component when = event.hour() == 0 ? t("events.when.day", dated.day()) : t("events.when", dated.day(), event.hour());
            lines.add(new EventLine(when, alertLine(new TownOperationalStatus.ActiveAlert(event.type(), event.severity(), event.affectedCount())), tone));
        }
        return lines;
    }

    private static Laws laws(TeamTown town, long day) {
        TownPolicyState state = town.getPolicyState();
        long cooldown = state.remainingCooldown(day, FHConfig.SERVER.TOWN.RESIDENT_RULES.townPolicyCooldownDays.get());
        List<Law> laws = new ArrayList<>();
        for (TownCareLaw law : TownCareLaw.values()) {
            String key = "gui.frostedheart.town_manager.policy." + law.name().toLowerCase(Locale.ROOT);
            boolean pending = state.hasPendingChanges() && state.displayedCareLaw() == law;
            laws.add(new Law(law, Component.translatable(key), Component.translatable(key + ".description"), state.careLaw() == law, pending));
        }
        Component line = state.hasPendingChanges() ? t("laws.pending") : cooldown > 0 ? t("laws.cooldown", cooldown) : t("laws.ready");
        return new Laws(t("laws.title"), laws, line, state.hasPendingChanges() ? Tone.WARN : cooldown > 0 ? Tone.MUTED : Tone.GOOD,
                cooldown > 0, List.of(t("laws.help.cooldown"), t("laws.help.next")));
    }
}
