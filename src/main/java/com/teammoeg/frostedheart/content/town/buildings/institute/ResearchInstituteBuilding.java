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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import static com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec.strict;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownTemperatureBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;

/**
 * 研究所的持久化建筑状态与每日工作。
 * <p>
 * A town research institute. Its bookshelves grant the team knowledge capacity while it works; each drawing desk
 * seats one researcher, whose labor, mostly from intelligence and raised by decoration, joins the town's daily
 * {@link VirtualResourceType#RESEARCH_LABOR} at the researcher's education level. The town then hands that labor to
 * the knowledge system. It works only when its room is lit, warm and large enough.
 */
public class ResearchInstituteBuilding extends AbstractTownResidentWorkBuilding implements ITownTemperatureBuilding {
    /**
     * 最近一次结算：工作人数、各教育等级的研究人力与停止原因。 / The last settlement: researchers who worked, their labor
     * by education level and why work stopped, if it did.
     */
    public record InstituteDailyReport(boolean hasData, int workerCount, List<Double> labor, TownProductionStopReason stopReason) {
        public static final InstituteDailyReport EMPTY =
                new InstituteDailyReport(false, 0, List.of(), TownProductionStopReason.NONE);
        public static final Codec<InstituteDailyReport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                strict(Codec.BOOL).optionalFieldOf("hasData", false).forGetter(InstituteDailyReport::hasData),
                strict(Codec.INT).optionalFieldOf("workerCount", 0).forGetter(InstituteDailyReport::workerCount),
                strict(Codec.DOUBLE.listOf()).optionalFieldOf("labor", List.of()).forGetter(InstituteDailyReport::labor),
                strict(TownProductionStopReason.CODEC).optionalFieldOf("stopReason", TownProductionStopReason.NONE)
                        .forGetter(InstituteDailyReport::stopReason)
        ).apply(instance, InstituteDailyReport::new));

        public InstituteDailyReport {
            workerCount = Math.max(0, workerCount);
            labor = labor == null ? List.of() : labor.stream().map(value -> Double.isFinite(value) ? Math.max(0, value) : 0).toList();
            stopReason = stopReason == null ? TownProductionStopReason.NONE : stopReason;
        }

        public double total() {
            return labor.stream().mapToDouble(Double::doubleValue).sum();
        }

        public double at(int education) {
            return education >= 0 && education < labor.size() ? labor.get(education) : 0;
        }
    }

    public static final MapCodec<ResearchInstituteBuilding> MAP_CODEC = AbstractTownBuilding.named(RecordCodecBuilder.mapCodec(t -> t.group(
            strict(BlockPos.CODEC).optionalFieldOf("pos", BlockPos.ZERO).forGetter(o -> o.pos),
            strict(Codec.BOOL).optionalFieldOf("initialized", false).forGetter(ResearchInstituteBuilding::isInitialized),
            strict(Codec.BOOL).optionalFieldOf("isStructureValid", false).forGetter(ResearchInstituteBuilding::isStructureValid),
            strict(OccupiedZoneInfo.CODEC).optionalFieldOf("occupiedVolume", OccupiedZoneInfo.EMPTY).forGetter(o -> o.getOccupiedVolume()),
            strict(UUIDUtil.CODEC.listOf()).optionalFieldOf("residentsID", List.of())
                    .forGetter(o -> o.getResidentsID().stream().sorted().toList()),
            strict(Codec.INT).optionalFieldOf("area", 0).forGetter(ResearchInstituteBuilding::getArea),
            strict(Codec.INT).optionalFieldOf("volume", 0).forGetter(ResearchInstituteBuilding::getVolume),
            strict(Codec.INT).optionalFieldOf("maxResidents", 0).forGetter(ResearchInstituteBuilding::getMaxResidents),
            strict(Codec.DOUBLE).optionalFieldOf("temperature", 0D).forGetter(ResearchInstituteBuilding::getTemperature),
            strict(Codec.DOUBLE).optionalFieldOf("temperatureModifier", 0D).forGetter(ResearchInstituteBuilding::getTemperatureModifier),
            strict(Codec.DOUBLE).optionalFieldOf("averageLight", 0D).forGetter(ResearchInstituteBuilding::getAverageLight),
            strict(Codec.INT).optionalFieldOf("bookshelves", 0).forGetter(ResearchInstituteBuilding::getBookshelves),
            strict(Codec.INT).optionalFieldOf("desks", 0).forGetter(ResearchInstituteBuilding::getDesks),
            strict(Codec.DOUBLE).optionalFieldOf("decorationRating", 0D).forGetter(ResearchInstituteBuilding::getDecorationRating),
            strict(InstituteDailyReport.CODEC).optionalFieldOf("dailyReport", InstituteDailyReport.EMPTY)
                    .forGetter(ResearchInstituteBuilding::getDailyReport)
    ).apply(t, ResearchInstituteBuilding::new)));
    public static final Codec<ResearchInstituteBuilding> CODEC = MAP_CODEC.codec();

    @Getter
    private int area;
    @Getter
    private int volume;
    @Getter
    private double temperature;
    @Getter
    private double temperatureModifier;
    /** Average block light over the room's standing positions, 0 to 15. */
    @Getter
    private double averageLight;
    @Getter
    private int bookshelves;
    /** Drawing desks in the room; each seats one researcher. */
    @Getter
    private int desks;
    /** Decoration rating, 0 to 1. */
    @Getter
    private double decorationRating;
    @Getter
    private InstituteDailyReport dailyReport = InstituteDailyReport.EMPTY;

    public ResearchInstituteBuilding(BlockPos pos) {
        super(pos);
    }

    public ResearchInstituteBuilding(BlockPos pos, boolean initialized, boolean structureValid, OccupiedZoneInfo occupiedVolume,
                                     List<UUID> residentsID, int area, int volume, int maxResidents, double temperature,
                                     double temperatureModifier, double averageLight, int bookshelves, int desks,
                                     double decorationRating, InstituteDailyReport dailyReport) {
        super(pos);
        setInitialized(initialized);
        setIsStructureValid(structureValid);
        internalSetOccupiedVolume(occupiedVolume);
        this.residentsID = new HashSet<>(residentsID);
        setArea(area);
        setVolume(volume);
        setMaxResidents(maxResidents);
        setTemperature(temperature);
        setTemperatureModifier(temperatureModifier);
        setAverageLight(averageLight);
        setBookshelves(bookshelves);
        setDesks(desks);
        setDecorationRating(decorationRating);
        this.dailyReport = dailyReport == null ? InstituteDailyReport.EMPTY : dailyReport;
    }

    public void setArea(int area) {
        if (this.area == area) return;
        this.area = area;
        fireChange();
    }

    public void setVolume(int volume) {
        if (this.volume == volume) return;
        this.volume = volume;
        fireChange();
    }

    public void setTemperature(double temperature) {
        if (Double.compare(this.temperature, temperature) == 0) return;
        this.temperature = temperature;
        fireChange();
    }

    public void setTemperatureModifier(double temperatureModifier) {
        if (Double.compare(this.temperatureModifier, temperatureModifier) == 0) return;
        this.temperatureModifier = temperatureModifier;
        fireChange();
    }

    public void setAverageLight(double averageLight) {
        double value = Double.isFinite(averageLight) ? Math.max(0, Math.min(15, averageLight)) : 0;
        if (Double.compare(this.averageLight, value) == 0) return;
        this.averageLight = value;
        fireChange();
    }

    /** Bookshelves set the team's knowledge capacity, so a change is an availability change. */
    public void setBookshelves(int bookshelves) {
        if (this.bookshelves == bookshelves) return;
        this.bookshelves = Math.max(0, bookshelves);
        fireAvailabilityChange();
    }

    public void setDesks(int desks) {
        if (this.desks == desks) return;
        this.desks = Math.max(0, desks);
        fireChange();
    }

    public void setDecorationRating(double decorationRating) {
        double value = Double.isFinite(decorationRating) ? Math.max(0, Math.min(1, decorationRating)) : 0;
        if (Double.compare(this.decorationRating, value) == 0) return;
        this.decorationRating = value;
        fireChange();
    }

    private void setDailyReport(InstituteDailyReport report) {
        InstituteDailyReport next = report == null ? InstituteDailyReport.EMPTY : report;
        if (next.equals(dailyReport)) return;
        dailyReport = next;
        fireChange();
    }

    @Override
    public boolean isBuildingWorkable() {
        return super.isBuildingWorkable() && isSpaceValid() && isTemperatureValid() && isLightValid();
    }

    /** An unworkable institute still reports why it did not work. */
    @Override
    public boolean shouldRunDailySettlement() {
        return true;
    }

    public boolean isSpaceValid() {
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        return area >= config.minimumFloorAreaBlocks.get() && volume >= config.minimumInteriorVolumeBlocks.get();
    }

    @Override
    public double getEffectiveTemperature() {
        return temperature + temperatureModifier;
    }

    public boolean isTemperatureValid() {
        if (ITown.DEBUG_MODE) return true;
        return getEffectiveTemperature() >= FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.minimumWorkingTemperatureCelsius.get();
    }

    public boolean isLightValid() {
        return averageLight + 1e-9 >= FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.minimumAverageBlockLight.get();
    }

    /** Archived observation records this institute grants its team while it works. */
    public int archiveCapacity() {
        return isBuildingWorkable() ? ResearchInstituteDailyModel.capacity(bookshelves,
                FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.archiveObservationsPerBookshelf.get()) : 0;
    }

    /** Inbox observation records this institute grants its team while it works. */
    public int inboxCapacity() {
        return isBuildingWorkable() ? ResearchInstituteDailyModel.capacity(bookshelves,
                FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.inboxObservationsPerBookshelf.get()) : 0;
    }

    @Override
    public boolean work(ITownWithBuildings town, ServerLevel world) {
        if (!(town instanceof TeamTown teamTown)) {
            throw new IllegalArgumentException("ResearchInstituteBuilding requires a TeamTown.");
        }
        if (!isBuildingWorkable()) {
            setDailyReport(new InstituteDailyReport(true, 0, List.of(), TownProductionStopReason.BUILDING_UNWORKABLE));
            return false;
        }
        List<Resident> researchers = eligibleResearchers(teamTown);
        if (researchers.isEmpty()) {
            setDailyReport(new InstituteDailyReport(true, 0, List.of(), TownProductionStopReason.NO_ELIGIBLE_WORKERS));
            return false;
        }
        List<Double> labor = laborByEducation(researchers);
        for (int education = 0; education < labor.size(); education++) {
            double amount = labor.get(education);
            if (amount <= TeamTownResourceHolder.DELTA) continue;
            teamTown.getActionExecutorHandler().execute(new TownResourceActions.VirtualResourceAttributeAction(
                    VirtualResourceType.RESEARCH_LABOR.generateAttribute(education), amount,
                    ResourceActionType.ADD, ResourceActionMode.ATTEMPT));
        }
        recordStudy(researchers);
        setDailyReport(new InstituteDailyReport(true, researchers.size(), labor, TownProductionStopReason.NONE));
        return true;
    }

    /**
     * Labor the current roster would give at the next settlement, by education level; empty when unworkable.
     */
    public List<Double> forecastLabor(TeamTown town) {
        if (!isBuildingWorkable()) return laborByEducation(List.of());
        return laborByEducation(eligibleResearchers(town));
    }

    @Override
    public double getResidentScore(Resident resident) {
        if (resident == null) return 0;
        return ResearchInstituteDailyModel.researcherLabor(researcherInput(resident), decorationRating, parameters());
    }

    /** Standard researchers this resident gives, at their education level. */
    public double getResearcherLabor(Resident resident) {
        return getResidentScore(resident);
    }

    private List<Double> laborByEducation(List<Resident> researchers) {
        double[] labor = new double[ResearchInstituteDailyModel.MAX_EDUCATION + 1];
        for (Resident researcher : researchers)
            labor[ResearchInstituteDailyModel.tier(researcher.getEducationLevel())] += getResidentScore(researcher);
        List<Double> result = new ArrayList<>(labor.length);
        for (double value : labor) result.add(value);
        return List.copyOf(result);
    }

    private List<Resident> eligibleResearchers(TeamTown town) {
        if (town == null) return List.of();
        return getResidents(town).stream()
                .filter(Objects::nonNull)
                .filter(this::canResidentWork)
                .sorted(Comparator.comparing(Resident::getUUID))
                .toList();
    }

    private static ResearchInstituteDailyModel.ResearcherInput researcherInput(Resident resident) {
        return new ResearchInstituteDailyModel.ResearcherInput(resident.getHealth(), resident.getMental(), resident.getStrength(),
                resident.getIntelligence(), resident.getWorkProficiency(ResearchInstituteBuilding.class));
    }

    static ResearchInstituteDailyModel.Parameters parameters() {
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        return new ResearchInstituteDailyModel.Parameters(
                config.healthWeight.get(),
                config.mentalWeight.get(),
                config.strengthWeight.get(),
                config.intelligenceWeight.get(),
                config.productivityAtAttributeZero.get(),
                config.productivityAtAttributeHundred.get(),
                config.maximumProficiency.get(),
                config.bonusAtMaximumProficiency.get(),
                config.minimumResidentProductivity.get(),
                config.maximumResidentProductivity.get(),
                config.decorationBonusAtFullRating.get());
    }

    /** Study trains intelligence more than any other job; research proficiency grows as elsewhere. */
    private static void recordStudy(List<Resident> researchers) {
        FHConfig.Server.Town.ResearchInstitute institute = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        FHConfig.Server.Town.ResidentProgression progression = FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
        for (Resident researcher : researchers) {
            researcher.recordDailyActivity(institute.physicalActivity.get(), institute.learningActivity.get());
            researcher.gainDailyWorkProficiency(ResearchInstituteBuilding.class,
                    progression.proficiencyGrowthAtZeroPerWorkday.get(),
                    progression.minimumProficiencyGrowthPerWorkday.get());
        }
    }
}
