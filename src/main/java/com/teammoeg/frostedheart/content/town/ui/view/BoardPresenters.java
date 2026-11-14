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

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionExplanation;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 各建筑 presenter 共用的规则：居民照片与人事卡、住处冷暖、页头与状态、工作条件标记，以及冷、暗、房间不合格、
 * 生病等常见便签。全是纯函数，文字只用整数与等级词。
 * <p>
 * Rules shared by the building presenters: resident photos and personnel cards, how warm a home is, the header
 * and status, condition chips, and the common notes (cold, dark, a bad room, a sick resident). All pure; text
 * uses whole numbers and level words only.
 */
public final class BoardPresenters {
    /** Below this margin above a working minimum a room counts as cool. */
    public static final double COOL_MARGIN = 5;
    private static final int[] PHOTO_COLORS = {0xFFCECBF6, 0xFF9FE1CB, 0xFFFAC775, 0xFFB5D4F4, 0xFFC0DD97, 0xFFF4C0D1};
    private static final int SICK_PHOTO = 0xFFF7C1C1;

    private BoardPresenters() {
    }

    public static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    public static Component education(int level) {
        return Component.translatable("gui.frostedheart.town_manager.education_level." + Math.max(0, Math.min(5, level)));
    }

    // ---------------------------------------------------------------- people

    public static String initials(Resident resident) {
        String first = resident.getFirstName() == null ? "" : resident.getFirstName().trim();
        String last = resident.getLastName() == null ? "" : resident.getLastName().trim();
        StringBuilder out = new StringBuilder();
        if (!first.isEmpty()) out.appendCodePoint(first.codePointAt(0));
        if (!last.isEmpty()) out.appendCodePoint(last.codePointAt(0));
        return out.toString().toUpperCase(Locale.ROOT);
    }

    public static String firstName(String name) {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    /** Whether a resident is healthy, spirited and housed enough to work, by the town's rules. */
    public static boolean fitToWork(Resident resident) {
        FHConfig.Server.Town.ResidentRules rules = FHConfig.SERVER.TOWN.RESIDENT_RULES;
        return resident.getHealth() > rules.minimumWorkingHealthExclusive.get()
                && resident.getMental() > rules.minimumWorkingMentalExclusive.get()
                && (!rules.workRequiresHousing.get() || resident.getHousePos() != null);
    }

    /** On duty when working; resting when too ill; off for any other reason. */
    public static Duty duty(Resident resident, boolean working) {
        if (working) return Duty.ON_DUTY;
        return resident.getHealth() <= FHConfig.SERVER.TOWN.RESIDENT_RULES.minimumWorkingHealthExclusive.get() ? Duty.RESTING : Duty.OFF;
    }

    /** Seen from home: working when fit with a job; free when fit without one or too young to work. */
    public static Duty homeDuty(Resident resident, Map<BlockPos, AbstractTownBuilding> buildings) {
        if (fitToWork(resident)) {
            AbstractTownBuilding job = resident.getWorkPos() == null ? null : buildings.get(resident.getWorkPos());
            if (job == null) return Duty.FREE;
            // A job at a stopped workplace is a day off, not a day at work.
            return job.isBuildingWorkable() ? Duty.ON_DUTY : Duty.OFF;
        }
        return duty(resident, false);
    }

    /**
     * 全镇人手的同一种分法：在岗、空闲、歇工、休养，外加还小的孩子。镇长印章的近况、居民页和岗位调度都用它，数目才对得上。
     * <p>
     * The one split of the town's people every headline and chip uses: on duty, free, off, resting, and children too
     * young to work, so the seal's overview, the residents page and staffing always agree.
     */
    public record Headcount(int total, int onDuty, int free, int off, int resting, int young) {
    }

    public static Headcount headcount(List<Resident> residents, Map<BlockPos, AbstractTownBuilding> buildings) {
        List<Person> people = new ArrayList<>();
        for (Resident resident : residents)
            people.add(new Person(resident.getUUID(), "", "", 0, Component.empty(), -1, homeDuty(resident, buildings),
                    Component.empty(), List.of(), List.of(), 0, resident.getAge()));
        return headcount(people);
    }

    public static Headcount headcount(List<Person> people) {
        int onDuty = 0, free = 0, off = 0, resting = 0, young = 0;
        int workingAge = FHConfig.SERVER.TOWN.RESIDENT_RULES.minimumWorkingAge.get();
        for (Person person : people) {
            switch (person.duty()) {
                case ON_DUTY -> onDuty++;
                case OFF -> off++;
                case RESTING -> resting++;
                case FREE -> {
                    if (person.age() < workingAge) young++;
                    else free++;
                }
            }
        }
        return new Headcount(people.size(), onDuty, free, off, resting, young);
    }

    public static Component dutyReason(Resident resident, Duty duty) {
        if (duty == Duty.ON_DUTY) return Component.empty();
        if (duty == Duty.RESTING) return b("person.reason.sick");
        // Fit and still off: the workplace is stopped.
        if (fitToWork(resident)) return b("person.reason.workplace_stopped");
        if (resident.getHousePos() == null && FHConfig.SERVER.TOWN.RESIDENT_RULES.workRequiresHousing.get()) return b("person.reason.homeless");
        if (resident.getMental() <= FHConfig.SERVER.TOWN.RESIDENT_RULES.minimumWorkingMentalExclusive.get()) return b("person.reason.low_mental");
        return b("person.reason.unfit");
    }

    public static Bar bar(String key, double value, boolean main) {
        int shown = (int) Math.floor(Math.max(0, Math.min(100, value)));
        Tone tone = shown <= 20 ? Tone.BAD : shown < 50 ? Tone.WARN : Tone.GOOD;
        // The colour always tells the value; the bar this job relies on is marked by its bold label instead.
        return new Bar(b("person.bar." + key), shown, main, tone);
    }

    /** What a day of this work trains: strength for physical work, intelligence for learning, and the skill itself. */
    public static Component growth(double physical, double learning, Component skill) {
        MutableComponent parts = Component.empty();
        if (physical > 0.05) parts.append(b("person.growth.strength")).append(" ");
        if (learning > 0.05) parts.append(b("person.growth.intelligence")).append(" ");
        parts.append(b("person.growth.skill", skill));
        return b("person.growth", parts);
    }

    /**
     * 一位居民的照片与人事卡。
     *
     * @param standardWorkers 折合的标准工人数；为负时不显示效率 / standard workers, negative to hide the efficiency
     * @param workType        熟练度所属的工作；为空时不画熟练度条 / the work whose proficiency is shown, null for none
     * @param mainAttribute   这份工作主要看的属性（strength、intelligence），没有则为空 / the attribute this work relies on
     */
    public static Person person(Resident resident, boolean working, double standardWorkers,
                                @Nullable Class<? extends ITownResidentWorkBuilding> workType, @Nullable Component skill,
                                @Nullable String mainAttribute, double physical, double learning, List<Component> extraLines,
                                Map<BlockPos, AbstractTownBuilding> buildings) {
        return person(resident, duty(resident, working), standardWorkers, workType, skill, mainAttribute, physical, learning,
                extraLines, buildings, true);
    }

    /**
     * {@link #person} with the duty given, and the home line left out when the card already sits in the home.
     * The variant for houses and the town overview, where residents are not seen through one workplace.
     */
    public static Person person(Resident resident, Duty duty, double standardWorkers,
                                @Nullable Class<? extends ITownResidentWorkBuilding> workType, @Nullable Component skill,
                                @Nullable String mainAttribute, double physical, double learning, List<Component> extraLines,
                                Map<BlockPos, AbstractTownBuilding> buildings, boolean homeLine) {
        boolean working = duty == Duty.ON_DUTY;
        List<Bar> bars = new ArrayList<>();
        bars.add(bar("health", resident.getHealth(), "health".equals(mainAttribute)));
        bars.add(bar("mental", resident.getMental(), "mental".equals(mainAttribute)));
        bars.add(bar("strength", resident.getStrength(), "strength".equals(mainAttribute)));
        bars.add(bar("intelligence", resident.getIntelligence(), "intelligence".equals(mainAttribute)));
        if (workType != null && skill != null) {
            double max = Math.max(1, FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION.maximumWorkProficiency.get());
            bars.add(new Bar(skill, (int) Math.floor(100 * resident.getWorkProficiency(workType) / max), false, Tone.NORMAL));
        }
        List<Component> lines = new ArrayList<>();
        if (skill != null) lines.add(working ? growth(physical, learning, skill) : b("person.growth.none"));
        lines.addAll(extraLines);
        Home home = home(resident, buildings);
        if (homeLine)
            lines.add(home == null ? b("person.homeless") : b("person.home", home.name(), home.warmth().homeText()));
        int color = duty == Duty.RESTING ? SICK_PHOTO : PHOTO_COLORS[Math.floorMod(resident.getUUID().hashCode(), PHOTO_COLORS.length)];
        return new Person(resident.getUUID(), resident.toString(), initials(resident), color, education(resident.getEducationLevel()),
                standardWorkers < 0 ? -1 : TownFormat.efficiency(standardWorkers), duty, dutyReason(resident, duty), bars, lines,
                home == null ? 0 : home.frost(), resident.getAge());
    }

    /** A nutrient by the name players know it: 主食, 肉食, 蔬菜, 油脂. */
    public static Component nutrient(ResidentNutritionSupportModel.Nutrient nutrient) {
        return b("nutrient." + nutrient.name().toLowerCase(Locale.ROOT));
    }

    /** The four nutrition reserves as bars, starting a new group: short below the healthy reserve, severe below 20. */
    public static List<Bar> nutritionBars(Resident resident) {
        double[] values = {resident.getNutrition().carbohydrate(), resident.getNutrition().protein(),
                resident.getNutrition().vegetable(), resident.getNutrition().fat()};
        ResidentNutritionSupportModel.Nutrient[] order = {ResidentNutritionSupportModel.Nutrient.CARBOHYDRATE,
                ResidentNutritionSupportModel.Nutrient.PROTEIN, ResidentNutritionSupportModel.Nutrient.VEGETABLE,
                ResidentNutritionSupportModel.Nutrient.FAT};
        double healthy = FHConfig.SERVER.TOWN.HOUSING.residentNutritionHealthyReserve.get();
        double severe = FHConfig.SERVER.TOWN.HOUSING.residentNutritionSevereReserve.get();
        List<Bar> bars = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            int shown = (int) Math.floor(Math.max(0, Math.min(100, values[i])));
            Tone tone = values[i] < severe ? Tone.BAD : values[i] < healthy ? Tone.WARN : Tone.GOOD;
            bars.add(new Bar(b("person.bar." + order[i].name().toLowerCase(Locale.ROOT)), shown, false, tone, i == 0));
        }
        return bars;
    }

    /** A resident's home as the player view needs it. */
    public record Home(Component name, TownFormat.Warmth warmth, int frost) {
    }

    @Nullable
    public static Home home(Resident resident, Map<BlockPos, AbstractTownBuilding> buildings) {
        BlockPos pos = resident.getHousePos();
        if (pos == null) return null;
        AbstractTownBuilding building = buildings.get(pos);
        if (!(building instanceof HouseBuilding house)) return new Home(b("person.home.unknown"), TownFormat.Warmth.WARM, 0);
        double temperature = house.getEffectiveTemperature();
        double minimum = FHConfig.SERVER.TOWN.HOUSING.minimumTemperatureCelsius.get();
        return new Home(TownBuildingLabels.name(buildings, pos, house), homeWarmth(temperature).warmth(), TownFormat.frost(temperature, minimum));
    }

    /** The one warmth verdict a home shows everywhere, from its current temperature (see {@link TownFormat#homeTemperature}). */
    public static TownFormat.Temperature homeWarmth(double celsius) {
        FHConfig.Server.Town.Housing housing = FHConfig.SERVER.TOWN.HOUSING;
        var scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        double comfortable = scoring.comfortableTemperatureCelsius.get();
        double rating = Double.isFinite(celsius) ? com.teammoeg.frostedheart.content.town.TownMathFunctions.calculateTemperatureRating(
                celsius, comfortable, scoring.minimumTemperatureRating.get(), scoring.temperatureRatingSlope.get(),
                scoring.temperatureRatingHalfPointDifferenceCelsius.get()) : 0;
        return TownFormat.homeTemperature(celsius, housing.minimumTemperatureCelsius.get(), housing.maximumTemperatureCelsius.get(),
                COOL_MARGIN, rating, comfortable);
    }

    /** A home's temperature chip, in the same word as its warmth tile. */
    public static Chip homeTemperatureChip(TownFormat.Temperature shown) {
        return new Chip(b("chip.temperature", shown.text(), shown.warmth().homeText()), warmthTone(shown.warmth()), -1);
    }

    /** Good when warm, a warning while livable but not comfortable or near the cold limit, bad when unlivable. */
    public static Tone warmthTone(TownFormat.Warmth warmth) {
        return switch (warmth) {
            case WARM -> Tone.GOOD;
            case COOL, CHILLY, STUFFY -> Tone.WARN;
            default -> Tone.BAD;
        };
    }

    /** The on-duty count over the posts, with the reason the rest are missing. */
    public static Stat workersStat(Component title, List<Person> people, int slots) {
        long onDuty = people.stream().filter(p -> p.duty() == Duty.ON_DUTY).count();
        long sick = people.stream().filter(p -> p.duty() == Duty.RESTING).count();
        long off = people.size() - onDuty - sick;
        Component sub;
        Tone tone;
        if (people.isEmpty()) {
            sub = b("stat.workers.none");
            tone = Tone.WARN;
        } else if (sick > 0) {
            sub = b("stat.workers.sick", sick);
            tone = Tone.BAD;
        } else if (off > 0) {
            sub = b("stat.workers.off", off);
            tone = Tone.BAD;
        } else if (people.size() < slots) {
            sub = b("stat.workers.empty", slots - people.size());
            tone = Tone.WARN;
        } else {
            sub = b("stat.workers.full");
            tone = Tone.MUTED;
        }
        return new Stat(title, Component.literal(onDuty + " / " + slots), sub, tone, List.of(), 1);
    }

    // ---------------------------------------------------------------- header and room

    /** The page head of a building's board: its name, a document line with its number and place in town, its status. */
    public static Header header(AbstractTownBuilding building, @Nullable TeamTown town, BlockPos pos,
                                String townName, long townDay, List<Component> stopped, List<Component> limited, int frost) {
        Map<BlockPos, AbstractTownBuilding> buildings = town == null ? Map.of() : town.getTownBuildings();
        int number = TownBuildingLabels.number(buildings, pos, building);
        Status status = !stopped.isEmpty() ? Status.STOPPED : limited.isEmpty() ? Status.RUNNING : Status.LIMITED;
        String direction = TownBuildingLabels.direction(town == null ? null : town.getTowerPos(), pos);
        Component docLine = direction == null
                ? b("doc_line", TownBuildingLabels.docMark(building), number, townName, townDay)
                : b("doc_line.where", TownBuildingLabels.docMark(building), number, b("dir." + direction), townName, townDay);
        return new Header(TownBuildingLabels.name(building, number), docLine, status, !stopped.isEmpty() ? stopped : limited, frost);
    }

    /** Why the room itself does not count: not checked yet, shared, not enclosed, too small. */
    public static List<Component> roomReasons(AbstractTownBuilding building, int area, int minArea, int volume, int minVolume) {
        List<Component> reasons = new ArrayList<>();
        if (!building.isInitialized()) reasons.add(b("reason.uninitialized"));
        if (building.isOccupiedAreaOverlapped()) reasons.add(b("reason.overlap"));
        if (!building.isStructureValid()) reasons.add(b("reason.structure"));
        if (area < minArea) reasons.add(b("reason.area", minArea));
        if (volume < minVolume) reasons.add(b("reason.volume", minVolume));
        return reasons;
    }

    /** What to do about the first problem {@link #roomReasons} names. */
    public static Component roomAction(AbstractTownBuilding building, int area, int minArea, int minVolume) {
        if (building.isOccupiedAreaOverlapped()) return b("note.structure.overlap");
        if (!building.isStructureValid() || !building.isInitialized()) return b("note.structure.walls");
        if (area < minArea) return b("note.structure.area", minArea);
        return b("note.structure.volume", minVolume);
    }

    public static Chip spaceChip(boolean valid, int people) {
        return valid ? new Chip(b("chip.space", people), Tone.GOOD, -1) : new Chip(b("chip.space.bad"), Tone.BAD, -1);
    }

    public static Chip temperatureChip(double celsius, double minimum, double maximum) {
        TownFormat.Temperature temperature = TownFormat.temperature(celsius, minimum, maximum, COOL_MARGIN);
        Tone tone = switch (temperature.warmth()) {
            case WARM -> Tone.GOOD;
            case COOL -> Tone.WARN;
            default -> Tone.BAD;
        };
        return new Chip(b("chip.temperature", temperature.text(), temperature.warmth().text()), tone, -1);
    }

    public static Chip lightChip(double average, double minimum) {
        TownFormat.Light light = TownFormat.light(average, minimum);
        return new Chip(light.text(), light == TownFormat.Light.DIM ? Tone.BAD : Tone.GOOD, -1);
    }

    public static Chip decorationChip(double rating) {
        int stars = TownFormat.stars(rating);
        return new Chip(b("chip.decoration"), stars >= 3 ? Tone.GOOD : Tone.WARN, stars);
    }

    // ---------------------------------------------------------------- notes

    public static Note improve(String problem, Component line, Component action, Component signature) {
        // A note is a resident's own words, so the room's fault is told as they feel it, not as an inspection report.
        if ("structure".equals(problem) && line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents reason
                && reason.getKey().equals(TownFormat.KEY + "reason.structure"))
            line = b("note.structure");
        return new Note(NoteKind.IMPROVE, problem, line, action, signature);
    }

    public static Note chat(Component line, Component signature) {
        return new Note(NoteKind.TALK, "", line, Component.empty(), signature);
    }

    /** Signed by an on-duty resident other than the one the note is about; a memo when nobody can sign. */
    public static Component signer(List<Person> people, @Nullable Person about) {
        for (Person person : people)
            if ((person.duty() == Duty.ON_DUTY || person.duty() == Duty.FREE) && person != about) return b("note.signed", firstName(person.name()));
        return b("note.memo");
    }

    /** A note when the room is below its working minimum or within {@link #COOL_MARGIN} of it; null otherwise. */
    @Nullable
    public static Note coldNote(double celsius, double minimum, Component signer) {
        int target = (int) Math.ceil(minimum);
        if (celsius < minimum) return improve("cold", b("note.freezing"), b("note.cold.action", target), signer);
        double margin = celsius - minimum;
        if (margin < COOL_MARGIN)
            return improve("cool", b("note.cool", (int) Math.max(1, Math.floor(margin))), b("note.cold.action", target), signer);
        return null;
    }

    /** Why a resident cannot work and what would help, from their home's warmth and their nutrition. */
    public static Note unfitNote(Resident resident, Person person, List<Person> people, Map<BlockPos, AbstractTownBuilding> buildings) {
        Component signer = signer(people, person);
        Home home = home(resident, buildings);
        if (home == null) return improve("homeless", b("note.homeless", person.name()), b("note.homeless.action"), signer);
        if (home.warmth() == TownFormat.Warmth.COLD)
            return improve("sick_cold", b("note.sick.cold", person.name(), home.name()), b("note.sick.cold.action", home.name()), signer);
        List<ResidentNutritionSupportModel.Nutrient> limits = ResidentNutritionExplanation.limitingNutrients(resident, 1);
        if (!limits.isEmpty()) {
            Component nutrient = nutrient(limits.get(0));
            return improve("sick_food", b("note.sick.food", person.name(), nutrient), b("note.sick.food.action", nutrient), signer);
        }
        if (person.duty() == Duty.RESTING) return improve("sick", b("note.sick", person.name()), b("note.sick.action"), signer);
        return improve("low_mental", b("note.low_mental", person.name()), b("note.low_mental.action"), signer);
    }

    /** At most {@code max} notes, in the order given. */
    public static List<Note> firstNotes(List<Note> notes, int max) {
        return notes.size() > max ? List.copyOf(notes.subList(0, max)) : List.copyOf(notes);
    }
}
