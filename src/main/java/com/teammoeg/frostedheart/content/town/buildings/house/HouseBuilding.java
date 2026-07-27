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
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;

import lombok.Getter;

import static com.teammoeg.frostedheart.content.town.ITown.DEBUG_MODE;
import static com.teammoeg.frostedheart.content.town.resource.ItemResourceType.RESIDENT_FOOD_LEVEL;

/**
 * 城镇住宅。
 * 它不继承AbstractTownResidentWorkBuilding，因为那个类用于需要居民参与工作的城镇建筑，但居民在房屋中并非工作。
 */
public class HouseBuilding extends AbstractTownBuilding implements ITownResidentBuilding {

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
            Codec.DOUBLE.optionalFieldOf("temperatureModifier",0D).forGetter(o -> o.getTemperatureModifier()),
            DailyReport.CODEC.optionalFieldOf("dailyReport", DailyReport.EMPTY).forGetter(o -> o.getDailyReport()))
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

    public void setArea(int area) { this.area = area; fireChange(); }
    public void setVolume(int volume) { this.volume = volume; fireChange(); }
    public void setTemperature(double temperature) { this.temperature = temperature; fireChange(); }
    public void setDecorationRating(double decorationRating) { this.decorationRating = decorationRating; fireChange(); }
    public void setMaxResidents(int maxResidents) { this.maxResidents = maxResidents; fireChange(); }
    public void setTemperatureModifier(double temperatureModifier) { this.temperatureModifier = temperatureModifier; fireChange(); }


    public HouseBuilding(BlockPos pos) {
        super(pos);
    }

    public HouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, int area, int volume, double temperature, double decorationRating, int maxResidents, double temperatureModifier) {
        this(pos, isStructureValid, occupiedVolume, false, false, area, volume, temperature,
                decorationRating, maxResidents, temperatureModifier, DailyReport.EMPTY);
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
            double temperatureModifier,
            DailyReport dailyReport
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
        this.setTemperatureModifier(temperatureModifier);
        this.dailyReport = dailyReport == null ? DailyReport.EMPTY : dailyReport;
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
        return effectiveTemperature >= TownMathFunctions.MIN_TEMP_HOUSE && effectiveTemperature <= TownMathFunctions.MAX_TEMP_HOUSE;
    }

    public boolean isTemperatureValid(){
        return isTemperatureValid(getEffectiveTemperature());
    }

    public double getRating(){
        return getComfortRating();
    }

    @Override
    public boolean isBuildingWorkable() {
        return super.isBuildingWorkable()
                && isTemperatureValid()
                && area >= 4
                && volume >= 8;
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

        double foodConsumed = 0.0;
        double nutritionSum = 0.0;
        for (ITownResourceAttributeActionResult<?> detail : result.details()) {
            if (detail instanceof TownResourceActionResults.ItemResourceAttributeCostActionResult itemResult) {
                foodConsumed += itemResult.totalModifiedAmount();
                for (Map.Entry<ItemStackResourceKey, Double> entry : itemResult.details().entrySet()) {
                    ItemStackResourceKey key = entry.getKey();
                    double amount = entry.getValue();
                    // 查找对应的营养配方并累加营养值
                    for (NutritionRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), NutritionRecipe.TYPE)) {
                        if (recipe.conform(key.getItem())) {
                            nutritionSum += (recipe.getNutrition().getNutritionValue() / 4.0) * amount;
                        }
                    }
                }
            }
        }
        return new FoodConsumption(foodConsumed, nutritionSum);
    }

    private DailyReport createDailyReport(int residentCount, FoodConsumption consumption) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        double foodRequired = residentCount * config.foodConsumptionPerResidentDay.get();
        double foodSatisfaction = HouseDailyModel.calculateFoodSatisfaction(
                foodRequired, consumption.foodConsumed());
        double nutritionQuality = HouseDailyModel.calculateNutritionQuality(
                consumption.nutritionValue(),
                consumption.foodConsumed(),
                config.nutritionReferencePerFoodUnit.get());
        double nutritionMultiplier = HouseDailyModel.calculateNutritionRecoveryMultiplier(
                nutritionQuality, config.minimumNutritionRecoveryMultiplier.get());
        double effectiveTemperature = getEffectiveTemperature();
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(effectiveTemperature);
        double spaceRating = TownMathFunctions.calculateSpaceRating(volume, area);
        double comfortRating = calculateComfortRating(temperatureRating, spaceRating, decorationRating);
        return new DailyReport(
                true,
                residentCount,
                foodRequired,
                consumption.foodConsumed(),
                foodSatisfaction,
                nutritionQuality,
                nutritionMultiplier,
                effectiveTemperature,
                temperatureRating,
                spaceRating,
                decorationRating,
                comfortRating
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
                report.temperatureRating(),
                report.comfortRating(),
                config.healthLossAtZeroFoodPerResidentDay.get(),
                config.mentalLossAtZeroFoodPerResidentDay.get(),
                config.maximumHealthRecoveryPerResidentDay.get(),
                config.maximumMentalRecoveryPerResidentDay.get()
        );
    }

    public double getTemperatureRating() {
        return TownMathFunctions.calculateTemperatureRating(getEffectiveTemperature());
    }

    public double getSpaceRating() {
        return TownMathFunctions.calculateSpaceRating(volume, area);
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

    private void setDailyReport(DailyReport dailyReport) {
        this.dailyReport = dailyReport == null ? DailyReport.EMPTY : dailyReport;
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
