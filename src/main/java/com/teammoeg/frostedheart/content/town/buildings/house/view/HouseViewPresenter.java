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

package com.teammoeg.frostedheart.content.town.buildings.house.view;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionExplanation;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSnapshot;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel.Nutrient;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 住宅的玩家视图：住了几个人、吃饱没有、舒不舒服、暖不暖和；每位居民的身体与营养；上一顿饭。纯函数，只出整数与等级词。
 * <p>
 * The house's player view: how many live here, whether they ate their fill, how comfortable and warm it is; each
 * resident's body and nutrition; the last meal. Pure; text holds whole numbers and level words only.
 */
public final class HouseViewPresenter {
    public static final String KEY = "gui.frostedheart.board.house.";
    /** The house's status stamp reads 宜居 / 凑合 / 不宜居 instead of 运转 / 受限 / 停工. */
    public static final String STATUS_KEY = KEY + "status.";
    public static final int TAB_RESIDENTS = 1, TAB_MEAL = 2;
    private static final int MAX_IMPROVEMENTS = 3, MAX_NOTES = 6;
    /** Comfort below this many stars is worth a note. */
    private static final int BARE_STARS = 3;

    public record Residents(Component headline, List<Person> people, Component slots, int emptySlots, Component empty) {
    }

    /**
     * @param satisfaction 吃饱的百分比；没开过饭时为 -1 / percent fed, -1 before the first meal
     */
    public record Meal(Component title, int satisfaction, Tone tone, Component summary, List<ItemLine> items, Component empty,
                       List<Component> tips, List<Bar> nutrition, Component nutritionLine) {
    }

    public record HouseView(Header header, List<Stat> stats, List<Chip> conditions, List<Note> notes, List<Note> talk,
                            Residents residents, Meal meal) {
    }

    public record Inputs(@Nullable HouseBuilding house, BlockPos pos, List<Resident> residents, @Nullable TeamTown town,
                         String townName, long townDay) {
    }

    /** Which part of comfort holds a house back the most; warmth misses either way from the comfortable temperature. */
    enum Weakest {
        COLD, HOT, SPACE, DECORATION
    }

    private HouseViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static HouseView present(Inputs in) {
        HouseBuilding house = in.house();
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        double minimum = config.minimumTemperatureCelsius.get(), maximum = config.maximumTemperatureCelsius.get();
        int frost = house == null ? 0 : TownFormat.frost(house.getEffectiveTemperature(), minimum);
        List<Person> people = new ArrayList<>();
        for (Resident resident : in.residents()) people.add(resident(resident, buildings, frost));
        if (house == null) {
            Header header = new Header(Component.translatable("gui.frostedheart.town_manager.building.HouseBuilding"), Component.empty(),
                    Status.STOPPED, List.of(b("reason.missing")), 0);
            return new HouseView(header, List.of(), List.of(), List.of(), List.of(),
                    new Residents(Component.empty(), people, Component.empty(), 0, b("reason.missing")), meal(null, in.residents()));
        }
        double temperature = house.getEffectiveTemperature();
        int area = house.getArea(), volume = house.getVolume();
        int minArea = config.minimumFloorAreaBlocks.get(), minVolume = config.minimumInteriorVolumeBlocks.get();
        HouseBuilding.DailyReport report = house.getDailyReport();
        TownFormat.Temperature shown = BoardPresenters.homeWarmth(temperature);
        int comfort = TownFormat.stars(house.getComfortRating());
        boolean hungry = report.hasData() && report.residentCount() > 0 && TownFormat.percent(report.foodSatisfaction()) < 100;

        // ---- status: a house that fails its room or temperature takes nobody new in
        List<Component> room = BoardPresenters.roomReasons(house, area, minArea, volume, minVolume);
        List<Component> stopped = new ArrayList<>(room);
        if (temperature < minimum) stopped.add(b("reason.cold", (int) Math.ceil(minimum)));
        else if (temperature > maximum) stopped.add(b("reason.hot", (int) Math.floor(maximum)));
        List<Component> limited = new ArrayList<>();
        if (hungry) limited.add(t("reason.hungry"));
        if (shown.warmth() == TownFormat.Warmth.COOL) limited.add(t("reason.cool"));
        Weakest weakest = weakest(house, shown.warmth());
        String weak = weakest.name().toLowerCase(Locale.ROOT);
        if (comfort < 2) limited.add(t("reason.bare." + weak));
        Header header = BoardPresenters.header(house, in.town(), in.pos(), in.townName(), in.townDay(), stopped, limited, frost);

        // ---- key figures
        int capacity = house.getMaxResidents();
        long sick = people.stream().filter(p -> p.duty() == Duty.RESTING).count();
        Stat living = new Stat(t("stat.residents"), Component.literal(people.size() + " / " + capacity),
                sick > 0 ? t("stat.residents.sick", sick) : people.isEmpty() ? t("stat.residents.none")
                        : people.size() < capacity ? t("stat.residents.free", capacity - people.size()) : t("stat.residents.full"),
                sick > 0 ? Tone.BAD : people.size() < capacity ? Tone.MUTED : Tone.GOOD, List.of(), TAB_RESIDENTS);
        Stat fed;
        boolean ate = report.hasData() && report.residentCount() > 0;
        if (!ate && people.isEmpty())
            // An empty house with no meal on record has none coming either, so it does not promise one.
            fed = new Stat(t("stat.last_meal"), b("work.stat.none"), t("stat.fed.nobody"), Tone.MUTED, List.of(t("stat.fed.tooltip")), TAB_MEAL);
        else if (!ate)
            fed = new Stat(t("stat.last_meal"), t("stat.fed.none"), t("stat.fed.none.sub"), Tone.MUTED, List.of(t("stat.fed.tooltip")), TAB_MEAL);
        else {
            int percent = TownFormat.percent(report.foodSatisfaction());
            fed = new Stat(t("stat.last_meal"), Component.literal(percent + "%"),
                    t(percent >= 100 ? "stat.fed.full" : percent >= 70 ? "stat.fed.almost" : "stat.fed.hungry"),
                    percent >= 100 ? Tone.GOOD : percent >= 70 ? Tone.WARN : Tone.BAD, List.of(t("stat.fed.tooltip")), TAB_MEAL);
        }
        // When warmth holds comfort back, the comfort line uses the warmth tile's own word.
        Component weakLine = weakest == Weakest.COLD || weakest == Weakest.HOT ? shown.warmth().homeText() : t("stat.comfort.weak." + weak);
        Stat cosy = new Stat(t("stat.comfort"), t("comfort." + comfort),
                comfort >= 4 ? t("stat.comfort.good") : weakLine,
                comfort >= 4 ? Tone.GOOD : comfort >= BARE_STARS ? Tone.MUTED : Tone.WARN,
                List.of(t("stat.comfort.tooltip"), t("stat.comfort.tooltip.effect")));
        Stat warmth = new Stat(t("stat.temperature"), shown.text(), shown.warmth().homeText(), BoardPresenters.warmthTone(shown.warmth()),
                List.of(t("stat.temperature.tooltip", (int) Math.ceil(minimum), (int) Math.floor(maximum)),
                        t("stat.temperature.tooltip.effect", (int) Math.round(FHConfig.SERVER.TOWN.BUILDING_SCORING.comfortableTemperatureCelsius.get()))));

        // ---- conditions: the room, then the parts of comfort
        List<Chip> chips = new ArrayList<>();
        chips.add(BoardPresenters.spaceChip(room.isEmpty(), capacity));
        chips.add(Chip.of(t("chip.beds", house.getBedCount()), Tone.NORMAL));
        chips.add(BoardPresenters.homeTemperatureChip(shown));
        int spaceStars = TownFormat.stars(house.getSpaceRating());
        chips.add(new Chip(t("chip.roomy"), spaceStars >= 3 ? Tone.GOOD : Tone.WARN, spaceStars, List.of(t("chip.roomy.tooltip"))));
        Chip decoration = BoardPresenters.decorationChip(house.getDecorationRating());
        chips.add(new Chip(decoration.text(), decoration.tone(), decoration.stars(), List.of(t("chip.decoration.tooltip"))));

        // ---- notes, most urgent first
        Component signer = BoardPresenters.signer(people, null);
        List<Note> notes = new ArrayList<>();
        if (!room.isEmpty())
            notes.add(BoardPresenters.improve("structure", room.get(0), BoardPresenters.roomAction(house, area, minArea, minVolume), signer));
        if (temperature < minimum)
            notes.add(BoardPresenters.improve("cold", t("note.cold"), b("note.cold.action", (int) Math.ceil(minimum)), signer));
        else if (temperature > maximum)
            notes.add(BoardPresenters.improve("hot", t("note.hot"), b("note.hot.action", (int) Math.floor(maximum)), signer));
        else if (shown.warmth() == TownFormat.Warmth.COOL)
            notes.add(BoardPresenters.improve("cool", t("note.cool", (int) Math.max(1, Math.floor(temperature - minimum))),
                    b("note.cold.action", (int) Math.ceil(minimum)), signer));
        if (hungry) notes.add(BoardPresenters.improve("hungry", t("note.hungry"), t("note.hungry.action"), signer));
        Nutrient shortest = commonShortage(in.residents());
        if (shortest != null) {
            Component nutrient = BoardPresenters.nutrient(shortest);
            // A lone resident writes the note about themselves.
            Component line = people.size() == 1 ? t("note.nutrition.one", nutrient) : t("note.nutrition", nutrient);
            notes.add(BoardPresenters.improve("nutrition", line, t("note.nutrition.action", nutrient), signer));
        }
        for (int i = 0; i < people.size(); i++) {
            Person person = people.get(i);
            if (person.duty() != Duty.RESTING && person.duty() != Duty.OFF) continue;
            // Fit and off means their workplace stopped; that is the workplace's note, not the home's.
            if (BoardPresenters.fitToWork(in.residents().get(i))) continue;
            Note unfit = BoardPresenters.unfitNote(in.residents().get(i), person, people, buildings);
            // The cold and nutrition notes already name these causes; the next unfit resident may have another.
            if (unfit.problem().equals("sick_cold") && temperature < minimum + BoardPresenters.COOL_MARGIN) continue;
            if (unfit.problem().equals("sick_food") && shortest != null) continue;
            notes.add(unfit);
            break;
        }
        if (comfort < BARE_STARS)
            notes.add(BoardPresenters.improve("bare", t("note.bare." + weak), t("note.bare.action." + weak), signer));
        notes = BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);

        // ---- small talk from the residents
        List<Note> talk = new ArrayList<>();
        if (people.isEmpty()) talk.add(BoardPresenters.chat(t(stopped.isEmpty() ? "talk.empty" : "talk.empty.unfit"), signer));
        else {
            if (shown.warmth() == TownFormat.Warmth.WARM) talk.add(BoardPresenters.chat(t("talk.warm"), signer));
            if (report.hasData() && !hungry) talk.add(BoardPresenters.chat(t("talk.fed"), signer));
            if (comfort >= 4) talk.add(BoardPresenters.chat(t("talk.cosy"), signer));
            if (people.size() < capacity && stopped.isEmpty()) talk.add(BoardPresenters.chat(t("talk.free_bed"), signer));
            for (int i = 1; i <= 4; i++) talk.add(BoardPresenters.chat(t("talk.daily." + i), signer));
        }
        int spare = Math.max(0, MAX_NOTES - notes.size());
        if (talk.size() > spare) talk = List.copyOf(talk.subList(0, spare));

        Component slots = people.size() < capacity
                ? stopped.isEmpty() ? t("residents.free", capacity - people.size()) : t("residents.free.unfit", capacity - people.size())
                : t("residents.full");
        Residents residents = new Residents(t("residents.headline", people.size(), capacity), people, slots,
                stopped.isEmpty() ? Math.max(0, capacity - people.size()) : 0, t(stopped.isEmpty() ? "residents.none" : "residents.none.unfit"));
        return new HouseView(header, List.of(living, fed, cosy, warmth), chips, notes, talk, residents, meal(report, in.residents()));
    }

    // ------------------------------------------------------------------ residents

    /** At home: working when fit with a job; free when fit without one or too young to work. */
    static Duty duty(Resident resident, Map<BlockPos, AbstractTownBuilding> buildings) {
        return BoardPresenters.homeDuty(resident, buildings);
    }

    private static Person resident(Resident resident, Map<BlockPos, AbstractTownBuilding> buildings, int frost) {
        Duty duty = duty(resident, buildings);
        List<Component> lines = new ArrayList<>();
        BlockPos work = resident.getWorkPos();
        AbstractTownBuilding job = work == null ? null : buildings.get(work);
        if (job != null) lines.add(t("person.job", TownBuildingLabels.name(buildings, work, job)));
        else if (resident.getAge() < FHConfig.SERVER.TOWN.RESIDENT_RULES.minimumWorkingAge.get()) lines.add(t("person.too_young"));
        else lines.add(t("person.no_job"));
        ResidentNutritionSnapshot snapshot = resident.getNutritionSnapshot();
        if (snapshot.hasData()) {
            List<Nutrient> limits = ResidentNutritionExplanation.limitingNutrients(resident, 1);
            lines.add(limits.isEmpty() ? t("person.nutrition.ok") : t("person.nutrition.short", BoardPresenters.nutrient(limits.get(0))));
            String health = direction(snapshot.recovery().healthNet()), mental = direction(snapshot.recovery().mentalNet());
            lines.add(health.equals(mental) && !health.equals("flat") ? t("person.trend.both." + health)
                    : t("person.trend", t("person.trend.health." + health), t("person.trend.mental." + mental)));
        }
        Person base = BoardPresenters.person(resident, duty, -1, null, null, null, 0, 0, lines, buildings, false);
        List<Bar> bars = new ArrayList<>(base.bars().subList(0, 2));
        bars.addAll(BoardPresenters.nutritionBars(resident));
        Component badge = Component.translatable(Resident.ageLangKey(resident.getAge())).copy().append(b("separator"))
                .append(BoardPresenters.education(resident.getEducationLevel()));
        return new Person(base.id(), base.name(), base.initials(), base.color(), badge, -1, duty, base.reason(), bars, base.lines(), frost, base.age());
    }

    private static String direction(double net) {
        return net > 0.05 ? "up" : net < -0.05 ? "down" : "flat";
    }

    /** The nutrient at least half the residents lack the most, or null. */
    @Nullable
    static Nutrient commonShortage(List<Resident> residents) {
        if (residents.isEmpty()) return null;
        Map<Nutrient, Integer> counts = new EnumMap<>(Nutrient.class);
        for (Resident resident : residents)
            for (Nutrient nutrient : ResidentNutritionExplanation.limitingNutrients(resident, 1))
                counts.merge(nutrient, 1, Integer::sum);
        Nutrient best = null;
        int most = 0;
        for (Map.Entry<Nutrient, Integer> entry : counts.entrySet())
            // A tie goes to the lowest reserve on average, which is what the meal tab's bars show lowest.
            if (entry.getValue() > most || entry.getValue() == most && best != null
                    && averageReserve(residents, entry.getKey()) < averageReserve(residents, best)) {
                best = entry.getKey();
                most = entry.getValue();
            }
        return most * 2 >= residents.size() ? best : null;
    }

    private static double averageReserve(List<Resident> residents, Nutrient nutrient) {
        double sum = 0;
        for (Resident resident : residents)
            sum += switch (nutrient) {
                case CARBOHYDRATE -> resident.getNutrition().carbohydrate();
                case PROTEIN -> resident.getNutrition().protein();
                case VEGETABLE -> resident.getNutrition().vegetable();
                case FAT -> resident.getNutrition().fat();
                default -> 100;
            };
        return sum / Math.max(1, residents.size());
    }

    /** The weakest of warmth, space and decoration, weighted as comfort weighs them. */
    static Weakest weakest(HouseBuilding house, TownFormat.Warmth warmth) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        // A home whose tile reads warm never names warmth as its weak point, so the two lines cannot disagree.
        double warmthGap = warmth == TownFormat.Warmth.WARM ? -1 : (1 - house.getTemperatureRating()) * config.temperatureComfortWeight.get();
        double spaceGap = (1 - house.getSpaceRating()) * config.spaceComfortWeight.get();
        double decorationGap = (1 - house.getDecorationRating()) * config.decorationComfortWeight.get();
        if (warmthGap >= spaceGap && warmthGap >= decorationGap)
            return house.getEffectiveTemperature() > FHConfig.SERVER.TOWN.BUILDING_SCORING.comfortableTemperatureCelsius.get()
                    ? Weakest.HOT : Weakest.COLD;
        return spaceGap >= decorationGap ? Weakest.SPACE : Weakest.DECORATION;
    }

    // ------------------------------------------------------------------ meal

    static Meal meal(@Nullable HouseBuilding.DailyReport report, List<Resident> residents) {
        List<Component> tips = List.of(t("meal.tip.balance"), t("meal.tip.hunger"), t("meal.tip.warehouse"));
        List<Bar> nutrition = averageNutrition(residents);
        // The line under the reserve bars names the lowest of those bars, when it is below a healthy reserve, so the
        // words and the bars above them always agree.
        Nutrient lowest = null;
        double lowestReserve = FHConfig.SERVER.TOWN.HOUSING.residentNutritionHealthyReserve.get();
        if (!residents.isEmpty())
            for (Nutrient nutrient : new Nutrient[]{Nutrient.CARBOHYDRATE, Nutrient.PROTEIN, Nutrient.VEGETABLE, Nutrient.FAT}) {
                double reserve = averageReserve(residents, nutrient);
                if (reserve < lowestReserve) {
                    lowest = nutrient;
                    lowestReserve = reserve;
                }
            }
        Component nutritionLine = residents.isEmpty() ? t("meal.nutrition.nobody")
                : lowest != null ? t("meal.nutrition.short", BoardPresenters.nutrient(lowest)) : t("meal.nutrition.ok");
        if (report == null || !report.hasData() || !report.meal().hasData())
            return new Meal(t("meal.title.none"), -1, Tone.MUTED, Component.empty(), List.of(), t("meal.none"), tips, nutrition, nutritionLine);
        List<ItemLine> items = new ArrayList<>();
        for (HouseBuilding.MealEntry entry : report.meal().entries()) {
            ItemStack stack = entry.item().toItemStack();
            if (stack.isEmpty()) continue;
            long count = Math.round(entry.amount());
            Component amount = count < 1 ? t("meal.a_little") : Component.literal("× " + count);
            items.add(new ItemLine(stack, stack.getHoverName(), amount, Tone.NORMAL));
        }
        // Nobody ate: there is no share to show, so the fed card stays off the board (-1, as before the first meal).
        int percent = report.residentCount() <= 0 ? -1 : TownFormat.percent(report.foodSatisfaction());
        Tone tone = percent >= 100 ? Tone.GOOD : percent >= 70 ? Tone.WARN : Tone.BAD;
        // Food units are not items, so the summary counts people rather than portions.
        Component summary = t("meal.summary", report.residentCount());
        // An empty plate means nobody lived here that day, not an empty warehouse, unless someone did and went hungry.
        Component empty = report.residentCount() <= 0 ? t("meal.empty.nobody") : t("meal.empty");
        return new Meal(t("meal.title", report.meal().settlementDay()), percent, tone, summary, items, empty, tips, nutrition,
                nutritionLine);
    }

    /** The residents' average reserve of each nutrient, toned like a resident's own bars; empty without residents. */
    static List<Bar> averageNutrition(List<Resident> residents) {
        if (residents.isEmpty()) return List.of();
        double[] sums = new double[4];
        List<List<Bar>> each = new ArrayList<>();
        for (Resident resident : residents) each.add(BoardPresenters.nutritionBars(resident));
        for (List<Bar> bars : each)
            for (int i = 0; i < 4; i++) sums[i] += bars.get(i).value();
        double healthy = FHConfig.SERVER.TOWN.HOUSING.residentNutritionHealthyReserve.get();
        double severe = FHConfig.SERVER.TOWN.HOUSING.residentNutritionSevereReserve.get();
        List<Bar> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int shown = (int) Math.floor(sums[i] / residents.size());
            Tone tone = shown < severe ? Tone.BAD : shown < healthy ? Tone.WARN : Tone.GOOD;
            out.add(new Bar(each.get(0).get(i).label(), shown, false, tone));
        }
        return out;
    }
}
