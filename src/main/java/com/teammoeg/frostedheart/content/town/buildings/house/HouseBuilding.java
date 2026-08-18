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

package com.teammoeg.frostedheart.content.town.buildings.house;

import java.util.*;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownTemperatureBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TownFoodNutritionModel;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import lombok.Getter;

import static com.teammoeg.frostedheart.content.town.ITown.DEBUG_MODE;
import static com.teammoeg.frostedheart.content.town.resource.ItemResourceType.RESIDENT_FOOD_LEVEL;

/**
 * 城镇住宅。
 * 它不继承AbstractTownResidentWorkBuilding，因为那个类用于需要居民参与工作的城镇建筑，但居民在房屋中并非工作。
 */
public class HouseBuilding extends AbstractTownBuilding implements ITownResidentBuilding, ITownTemperatureBuilding {

    private static final long[] NO_BED_POSITIONS = new long[0];
    private static final Codec<long[]> BED_POSITIONS_CODEC = Codec.LONG_STREAM.xmap(
            positions -> canonicalizeBedPositions(positions.toArray()),
            positions -> Arrays.stream(positions));

    public record DailyReport(
            boolean hasData,
            int residentCount,
            double foodRequired,
            double foodConsumed,
            double foodSatisfaction,
            double nutritionQuality,
            double nutritionRecoveryMultiplier,
            double effectiveTemperature,
            double temperatureRating,
            double spaceRating,
            double decorationRating,
            double comfortRating
    ) {
        public static final DailyReport EMPTY = new DailyReport(
                false, 0, 0.0, 0.0, 1.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0);

        public static final Codec<DailyReport> CODEC = RecordCodecBuilder.create(t -> t.group(
                Codec.BOOL.optionalFieldOf("hasData", false).forGetter(DailyReport::hasData),
                Codec.INT.optionalFieldOf("residentCount", 0).forGetter(DailyReport::residentCount),
                Codec.DOUBLE.optionalFieldOf("foodRequired", 0.0).forGetter(DailyReport::foodRequired),
                Codec.DOUBLE.optionalFieldOf("foodConsumed", 0.0).forGetter(DailyReport::foodConsumed),
                Codec.DOUBLE.optionalFieldOf("foodSatisfaction", 1.0).forGetter(DailyReport::foodSatisfaction),
                Codec.DOUBLE.optionalFieldOf("nutritionQuality", 0.0).forGetter(DailyReport::nutritionQuality),
                Codec.DOUBLE.optionalFieldOf("nutritionRecoveryMultiplier", 0.0).forGetter(DailyReport::nutritionRecoveryMultiplier),
                Codec.DOUBLE.optionalFieldOf("effectiveTemperature", 0.0).forGetter(DailyReport::effectiveTemperature),
                Codec.DOUBLE.optionalFieldOf("temperatureRating", 0.0).forGetter(DailyReport::temperatureRating),
                Codec.DOUBLE.optionalFieldOf("spaceRating", 0.0).forGetter(DailyReport::spaceRating),
                Codec.DOUBLE.optionalFieldOf("decorationRating", 0.0).forGetter(DailyReport::decorationRating),
                Codec.DOUBLE.optionalFieldOf("comfortRating", 0.0).forGetter(DailyReport::comfortRating)
        ).apply(t, DailyReport::new));
    }

    public static final Codec<HouseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
            BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
            Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid()),
            OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.getOccupiedVolume()),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(o -> o.isInitialized()),
            Codec.BOOL.optionalFieldOf("occupiedAreaOverlapped", false).forGetter(o -> o.isOccupiedAreaOverlapped()),
            Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.getArea()),
            Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.getVolume()),
            Codec.DOUBLE.optionalFieldOf("temperature",0D).forGetter(o -> o.getTemperature()),
            Codec.DOUBLE.optionalFieldOf("decorationRating",0D).forGetter(o -> o.getDecorationRating()),
            Codec.INT.optionalFieldOf("maxResident",0).forGetter(o -> o.getMaxResidents()),
            Codec.list(UUIDUtil.CODEC).optionalFieldOf("residentsUUID",List.of()).forGetter(o -> new ArrayList<>(o.residentsUUID)),
            Codec.DOUBLE.optionalFieldOf("temperatureModifier",0D).forGetter(o -> o.getTemperatureModifier()),
            DailyReport.CODEC.optionalFieldOf("dailyReport", DailyReport.EMPTY).forGetter(o -> o.getDailyReport()),
            BED_POSITIONS_CODEC.optionalFieldOf("bedPositions", NO_BED_POSITIONS).forGetter(o -> o.bedPositions),
            Codec.LONG.optionalFieldOf("entrancePosition").forGetter(o ->
                    o.hasEntrance ? Optional.of(o.entrancePosition) : Optional.empty()))
            .apply(t, HouseBuilding::new));

    private final Set<UUID> residentsUUID = new HashSet<>();

    /**
     * 住宅的有效面积
     */
    @Getter
    private int area;
    /**
     * 住宅的体积
     */
    @Getter
    private int volume;
    /**
     * 住宅内部平均温度
     */
    @Getter
    private double temperature;
    /**
     * 住宅内部装饰物的综合评分，取值范围为0-1
     */
    @Getter
    private double decorationRating;
    /**
     * 最大可居住居民数
     */
    private int maxResidents;
    /**
     * 温度修正系数，用于调节房屋内温度效果
     */
    @Getter
    private double temperatureModifier;
    @Getter
    private DailyReport dailyReport = DailyReport.EMPTY;
    private long[] bedPositions = NO_BED_POSITIONS;
    private long entrancePosition;
    private boolean hasEntrance;

    public void setArea(int area) { if (this.area == area) return; this.area = area; fireChange(); }
    public void setVolume(int volume) { if (this.volume == volume) return; this.volume = volume; fireChange(); }
    public void setTemperature(double temperature) { if (Double.compare(this.temperature, temperature) == 0) return; this.temperature = temperature; fireChange(); }
    public void setDecorationRating(double decorationRating) { if (Double.compare(this.decorationRating, decorationRating) == 0) return; this.decorationRating = decorationRating; fireChange(); }
    public void setMaxResidents(int maxResidents) { if (this.maxResidents == maxResidents) return; this.maxResidents = maxResidents; fireChange(); }
    public void setTemperatureModifier(double temperatureModifier) { if (Double.compare(this.temperatureModifier, temperatureModifier) == 0) return; this.temperatureModifier = temperatureModifier; fireChange(); }


    public HouseBuilding(BlockPos pos) {
        super(pos);
    }

    public HouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, int area, int volume, double temperature, double decorationRating, int maxResidents, double temperatureModifier) {
        this(pos, isStructureValid, occupiedVolume, false, false, area, volume, temperature,
                decorationRating, maxResidents, List.of(), temperatureModifier, DailyReport.EMPTY);
    }

    public HouseBuilding(
            BlockPos pos,
            boolean isStructureValid,
            OccupiedVolume occupiedVolume,
            boolean initialized,
            boolean occupiedAreaOverlapped,
            int area,
            int volume,
            double temperature,
            double decorationRating,
            int maxResidents,
            List<UUID> residentsUUID,
            double temperatureModifier,
            DailyReport dailyReport
    ) {
        this(pos, isStructureValid, occupiedVolume, initialized, occupiedAreaOverlapped, area, volume,
                temperature, decorationRating, maxResidents, residentsUUID, temperatureModifier,
                dailyReport, NO_BED_POSITIONS, Optional.empty());
    }

    private HouseBuilding(
            BlockPos pos,
            boolean isStructureValid,
            OccupiedVolume occupiedVolume,
            boolean initialized,
            boolean occupiedAreaOverlapped,
            int area,
            int volume,
            double temperature,
            double decorationRating,
            int maxResidents,
            List<UUID> residentsUUID,
            double temperatureModifier,
            DailyReport dailyReport,
            long[] bedPositions,
            Optional<Long> entrancePosition
    ) {
        super(pos);
        this.setIsStructureValid(isStructureValid);
        this.setOccupiedVolume(occupiedVolume);
        this.setInitialized(initialized);
        this.setOccupiedAreaOverlapped(occupiedAreaOverlapped);
        this.setArea(area);
        this.setVolume(volume);
        this.setTemperature(temperature);
        this.setDecorationRating(decorationRating);
        this.setMaxResidents(maxResidents);
        this.residentsUUID.addAll(residentsUUID);
        this.setTemperatureModifier(temperatureModifier);
        this.dailyReport = dailyReport == null ? DailyReport.EMPTY : dailyReport;
        this.bedPositions = canonicalizeBedPositions(bedPositions);
        this.hasEntrance = entrancePosition.isPresent();
        this.entrancePosition = entrancePosition.orElse(0L);
    }

    /**
     * 测试用构造方法，不包含OccupiedVolume
     */
    public HouseBuilding(BlockPos pos, boolean isStructureValid, int area, int volume, double temperature, double decorationRating, int maxResidents, double temperatureModifier) {
        super(pos);
        this.setIsStructureValid(isStructureValid);
        this.setArea(area);
        this.setVolume(volume);
        this.setTemperature(temperature);
        this.setDecorationRating(decorationRating);
        this.setMaxResidents(maxResidents);
        this.setTemperatureModifier(temperatureModifier);
    }


    public boolean addResident(Resident resident) {
        resident.setHousePos(this.getPos());
        boolean added = residentsUUID.add(resident.getUUID());
        if (added) fireChange();
        return added;
    }

    public boolean removeResident(Resident resident){
        boolean removed = residentsUUID.remove(resident.getUUID());
        if (removed) fireChange();
        return removed;
    }

    @Override
    public int getMaxResidents() {
        return maxResidents;
    }

    public int getBedCount() {
        return bedPositions.length;
    }

    public long getBedPositionLong(int index) {
        return bedPositions[index];
    }

    public boolean hasEntrance() {
        return hasEntrance;
    }

    public long getEntrancePositionLong() {
        if (!hasEntrance) {
            throw new IllegalStateException("House does not have a scanned entrance");
        }
        return entrancePosition;
    }

    void setLayout(Collection<BlockPos> beds, BlockPos entrance) {
        Objects.requireNonNull(beds, "beds");
        Objects.requireNonNull(entrance, "entrance");
        long[] positions = beds.stream()
                .mapToLong(BlockPos::asLong)
                .toArray();
        setLayout(positions, true, entrance.asLong());
    }

    void clearLayout() {
        setLayout(NO_BED_POSITIONS, false, 0L);
    }

    private void setLayout(long[] beds, boolean hasEntrance, long entrance) {
        long[] canonicalBeds = canonicalizeBedPositions(beds);
        if (Arrays.equals(this.bedPositions, canonicalBeds)
                && this.hasEntrance == hasEntrance
                && (!hasEntrance || this.entrancePosition == entrance)) {
            return;
        }
        this.bedPositions = canonicalBeds;
        this.hasEntrance = hasEntrance;
        this.entrancePosition = hasEntrance ? entrance : 0L;
        fireChange();
    }

    private static long[] canonicalizeBedPositions(long[] positions) {
        if (positions == null || positions.length == 0) {
            return NO_BED_POSITIONS;
        }
        long[] sorted = positions.clone();
        Arrays.sort(sorted);
        int uniqueCount = 1;
        for (int readIndex = 1; readIndex < sorted.length; readIndex++) {
            if (sorted[readIndex] != sorted[uniqueCount - 1]) {
                sorted[uniqueCount++] = sorted[readIndex];
            }
        }
        return uniqueCount == sorted.length ? sorted : Arrays.copyOf(sorted, uniqueCount);
    }

    @Override
    public Collection<UUID> getResidentsID() {
        return residentsUUID;
    }

    @Override
    public Collection<Resident> getResidents(ITownWithResidents townOfBuilding) {
        return residentsUUID.stream()
                .map(townOfBuilding::getResident)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static boolean isTemperatureValid(double effectiveTemperature){
        if (DEBUG_MODE) return true;
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        return effectiveTemperature >= config.minimumTemperatureCelsius.get()
                && effectiveTemperature <= config.maximumTemperatureCelsius.get();
    }

    public boolean isTemperatureValid(){
        return isTemperatureValid(getEffectiveTemperature());
    }

    public double getRating(){
        return getComfortRating();
    }

    @Override
    public boolean isBuildingWorkable() {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        return HouseDailyModel.isBuildingWorkable(
                super.isBuildingWorkable(), area, volume, isTemperatureValid(),
                config.minimumFloorAreaBlocks.get(),
                config.minimumInteriorVolumeBlocks.get());
    }

    /**
     * A valid house remains responsible for its existing residents even when
     * its temperature is outside the habitable range. Temperature still makes
     * the house unworkable for allocation/UI purposes, but must not suspend
     * food consumption and resident health/mental settlement for free.
     */
    @Override
    public boolean shouldRunDailySettlement() {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        return HouseDailyModel.shouldRunDailySettlement(
                super.isBuildingWorkable(), area, volume,
                config.minimumFloorAreaBlocks.get(),
                config.minimumInteriorVolumeBlocks.get());
    }


    @Override
    public boolean work(ITownWithBuildings buildingTown) {
        if (!(buildingTown instanceof ITown town)) {
            FHMain.LOGGER.error("HouseBuilding: town is not a complete town!");
            return false;
        }

        Collection<Resident> residents = getResidents(town);
        int residentNum = residents.size();
        if (residentNum == 0) {
            setDailyReport(createDailyReport(0, new FoodConsumption(0.0, 0.0)));
            return true;
        }

        IActionExecutorHandler executorHandler = town.getActionExecutorHandler();
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        double foodRequired = residentNum * config.foodConsumptionPerResidentDay.get();
        FoodConsumption consumption = consumeFoodAndComputeNutrition(executorHandler, foodRequired);
        DailyReport report = createDailyReport(residentNum, consumption);
        setDailyReport(report);

        for (Resident resident : residents) {
            applyResidentEffects(resident, calculateResidentEffects(resident, report));
        }
        return true;
    }

    /**
     * 执行食物消耗动作，并累计所有消耗物品的营养值。
     * @return 实际消耗的食物资源量和营养值总和
     */
    private FoodConsumption consumeFoodAndComputeNutrition(IActionExecutorHandler executorHandler, double toCost) {
        TownResourceActions.TownResourceTypeCostAction action = new TownResourceActions.TownResourceTypeCostAction(
                RESIDENT_FOOD_LEVEL, Math.max(0.0, toCost), 0, 4,
                ResourceActionMode.MAXIMIZE, ResourceActionOrder.DESCENDING);
        TownResourceActionResults.TownResourceTypeCostActionResult result = executorHandler.execute(action);

        // 食谱表与方法内恒定（同一方法内 /reload 不会中途执行），提到循环外只重建一次
        List<NutritionRecipe> recipes = CUtils.filterRecipes(CDistHelper.getRecipeManager(), NutritionRecipe.TYPE);

        double foodConsumed = 0.0;
        double nutritionSum = 0.0;
        for (ITownResourceAttributeActionResult<?> detail : result.details()) {
            if (detail instanceof TownResourceActionResults.ItemResourceAttributeCostActionResult itemResult) {
                foodConsumed += itemResult.totalModifiedAmount();
                for (Map.Entry<ItemStackResourceKey, Double> entry : itemResult.details().entrySet()) {
                    ItemStackResourceKey key = entry.getKey();
                    double amount = entry.getValue();
                    nutritionSum += TownFoodNutritionModel.getNutritionPerItem(key, recipes) * amount;
                }
            }
        }
        return new FoodConsumption(foodConsumed, nutritionSum);
    }

    private DailyReport createDailyReport(int residentCount, FoodConsumption consumption) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        HouseDailyModel.SettlementReport modelReport = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        residentCount,
                        consumption.foodConsumed(),
                        consumption.nutritionValue(),
                        getEffectiveTemperature(),
                        area,
                        volume,
                        decorationRating),
                new HouseDailyModel.SettlementParameters(
                        config.foodConsumptionPerResidentDay.get(),
                        config.nutritionReferencePerFoodUnit.get(),
                        config.minimumNutritionRecoveryMultiplier.get(),
                        scoring.comfortableTemperatureCelsius.get(),
                        scoring.minimumTemperatureRating.get(),
                        scoring.temperatureRatingSlope.get(),
                        scoring.temperatureRatingHalfPointDifferenceCelsius.get(),
                        scoring.spaceAreaCoefficient.get(),
                        scoring.spaceHeightLogCoefficient.get(),
                        scoring.spaceHeightLogOffset.get(),
                        scoring.spaceResponseScale.get(),
                        scoring.spaceResponseExponent.get(),
                        config.temperatureComfortWeight.get(),
                        config.spaceComfortWeight.get(),
                        config.decorationComfortWeight.get()));
        return new DailyReport(
                true,
                modelReport.residentCount(),
                modelReport.foodRequired(),
                modelReport.foodConsumed(),
                modelReport.foodSatisfaction(),
                modelReport.nutritionQuality(),
                modelReport.nutritionRecoveryMultiplier(),
                modelReport.effectiveTemperature(),
                modelReport.temperatureRating(),
                modelReport.spaceRating(),
                modelReport.decorationRating(),
                modelReport.comfortRating()
        );
    }

    public HouseDailyModel.ResidentEffects calculateResidentEffects(Resident resident) {
        return calculateResidentEffects(resident, dailyReport);
    }

    private HouseDailyModel.ResidentEffects calculateResidentEffects(Resident resident, DailyReport report) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        return HouseDailyModel.calculateResidentEffects(
                resident.getHealth(),
                resident.getMental(),
                report.foodSatisfaction(),
                report.nutritionRecoveryMultiplier(),
                report.effectiveTemperature(),
                report.temperatureRating(),
                report.comfortRating(),
                new HouseDailyModel.ResidentEffectParameters(
                        config.foodDeficitPenaltyExponent.get(),
                        config.healthLossAtZeroFoodPerResidentDay.get(),
                        config.mentalLossAtZeroFoodPerResidentDay.get(),
                        config.minimumTemperatureCelsius.get(),
                        config.maximumTemperatureCelsius.get(),
                        config.temperatureFullStressDistanceCelsius.get(),
                        config.temperatureStressPenaltyExponent.get(),
                        config.healthLossAtFullTemperatureStressPerResidentDay.get(),
                        config.mentalLossAtFullTemperatureStressPerResidentDay.get(),
                        config.maximumHealthRecoveryPerResidentDay.get(),
                        config.maximumMentalRecoveryPerResidentDay.get())
        );
    }

    public double getTemperatureRating() {
        return calculateTemperatureRating(getEffectiveTemperature());
    }

    public double getSpaceRating() {
        return calculateSpaceRating(volume, area);
    }

    public double getComfortRating() {
        return calculateComfortRating(getTemperatureRating(), getSpaceRating(), decorationRating);
    }

    private static double calculateComfortRating(
            double temperatureRating,
            double spaceRating,
            double decorationRating
    ) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        return HouseDailyModel.calculateComfortRating(
                temperatureRating,
                spaceRating,
                decorationRating,
                config.temperatureComfortWeight.get(),
                config.spaceComfortWeight.get(),
                config.decorationComfortWeight.get()
        );
    }

    private static double calculateTemperatureRating(double temperature) {
        FHConfig.Server.Town.BuildingScoring config = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        return TownMathFunctions.calculateTemperatureRating(
                temperature,
                config.comfortableTemperatureCelsius.get(),
                config.minimumTemperatureRating.get(),
                config.temperatureRatingSlope.get(),
                config.temperatureRatingHalfPointDifferenceCelsius.get());
    }

    private static double calculateSpaceRating(int volume, int area) {
        FHConfig.Server.Town.BuildingScoring config = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        return TownMathFunctions.calculateSpaceRating(
                volume,
                area,
                config.spaceAreaCoefficient.get(),
                config.spaceHeightLogCoefficient.get(),
                config.spaceHeightLogOffset.get(),
                config.spaceResponseScale.get(),
                config.spaceResponseExponent.get());
    }

    private void setDailyReport(DailyReport dailyReport) {
        DailyReport report = dailyReport == null ? DailyReport.EMPTY : dailyReport;
        // 值级守卫：DailyReport 为 record（逐字段比较，无随机/时钟字段），空房等未变化
        // 场景连续两天报告相等——不再 fireChange，避免每日建筑包重发全部房屋条目
        if (Objects.equals(this.dailyReport, report)) return;
        this.dailyReport = report;
        fireChange();
    }

    private static void applyResidentEffects(
            Resident resident,
            HouseDailyModel.ResidentEffects effects
    ) {
        resident.setHealth(clampResidentAttribute(resident.getHealth() + effects.healthDelta()));
        resident.setMental(clampResidentAttribute(resident.getMental() + effects.mentalDelta()));
    }

    private static double clampResidentAttribute(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, value));
    }

    private record FoodConsumption(double foodConsumed, double nutritionValue) {
    }

    @Override
    public void onRemoved(ITownWithBuildings buildingTown) {
        if(buildingTown instanceof ITownWithResidents residentTown){
            for(UUID uuid : residentsUUID){
                residentTown.getResident(uuid).ifPresent(resident -> resident.setHousePos(null));
            }
        }
    }

    public double getEffectiveTemperature() {
        return temperature + temperatureModifier;
    }
}
