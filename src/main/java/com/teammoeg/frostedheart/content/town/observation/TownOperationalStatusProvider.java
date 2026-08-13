/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorFuelModel;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WeatherForecast;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownTemperatureBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.ResearchVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Adapts current gameplay objects to the shared operational equations. */
public final class TownOperationalStatusProvider {
    private TownOperationalStatusProvider() {
    }

    public static TownOperationalStatus capture(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        var teamData = CTeamDataManager.get(player);
        TeamTown town = TeamTown.from(player);
        return capture(world, teamData, town, player.blockPosition());
    }

    public static TownOperationalStatus capture(
            ServerLevel world,
            com.teammoeg.chorda.dataholders.team.TeamDataHolder teamData,
            TeamTown town,
            BlockPos fallbackClimatePosition
    ) {
        FHConfig.Server.Town.ResidentRules residentConfig = FHConfig.SERVER.TOWN.RESIDENT_RULES;

        List<TownObservationModel.ResidentStatus> residentStatuses = town.getAllResidents().stream()
                .map(resident -> new TownObservationModel.ResidentStatus(
                        resident.getUUID().toString(), resident.getAge(), resident.getHealth(), resident.getMental(),
                        resident.getHousePos() != null))
                .toList();
        TownObservationModel.ResidentSnapshot residents = TownObservationModel.observeResidents(
                residentStatuses,
                new TownObservationModel.ResidentRules(
                        residentConfig.homelessHealthLossPerDay.get(),
                        residentConfig.removalHealthThreshold.get(),
                        residentConfig.removalMentalThreshold.get(),
                        residentConfig.minimumWorkingAge.get(),
                        residentConfig.minimumWorkingHealthExclusive.get(),
                        residentConfig.minimumWorkingMentalExclusive.get(),
                        residentConfig.workRequiresHousing.get()));

        int homeless = 0;
        int unemployed = 0;
        for (Resident resident : town.getAllResidents()) {
            if (resident.getHousePos() == null) homeless++;
            if (resident.getWorkPos() == null) unemployed++;
        }

        TownOperationalStatus.Metric minHouseTemperature = TownOperationalStatus.Metric.unavailable();
        TownOperationalStatus.Metric minBuildingTemperature = TownOperationalStatus.Metric.unavailable();
        double houseMinimum = Double.POSITIVE_INFINITY;
        double buildingMinimum = Double.POSITIVE_INFINITY;
        int unsafeHouses = 0;
        int stoppedHunting = 0;
        for (AbstractTownBuilding building : town.getTownBuildings().values()) {
            if (building instanceof ITownTemperatureBuilding temperatureBuilding) {
                buildingMinimum = Math.min(buildingMinimum, temperatureBuilding.getEffectiveTemperature());
            }
            if (building instanceof HouseBuilding house && !house.getResidentsID().isEmpty()) {
                double temperature = house.getEffectiveTemperature();
                houseMinimum = Math.min(houseMinimum, temperature);
                if (!house.isTemperatureValid()) unsafeHouses++;
            } else if (building instanceof HuntingBaseBuilding hunting && !hunting.getResidentsID().isEmpty()) {
                if (!hunting.isTemperatureValid()) stoppedHunting++;
            }
        }
        if (Double.isFinite(houseMinimum)) minHouseTemperature = TownOperationalStatus.Metric.available(houseMinimum);
        if (Double.isFinite(buildingMinimum)) {
            minBuildingTemperature = TownOperationalStatus.Metric.available(buildingMinimum);
        }

        TownOperationalStatus.Metric foodReserve = TownOperationalStatusModel.foodReserveDays(
                town.getResourceHolder().get(ItemResourceType.RESIDENT_FOOD_LEVEL),
                residents.population(), FHConfig.SERVER.TOWN.HOUSING.foodConsumptionPerResidentDay.get());

        GeneratorData generator = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA).orElse(null);
        ServerLevel towerWorld = generator != null && generator.dimension != null
                ? world.getServer().getLevel(generator.dimension) : world;
        if (towerWorld == null) towerWorld = world;
        TownOperationalStatus.TowerStatus tower = captureTower(towerWorld, generator);
        TownOperationalStatus.Metric fuelReserve = tower.kind() == TownOperationalStatus.TowerKind.T1
                ? captureT1FuelReserve(towerWorld, teamData, town, generator)
                : TownOperationalStatus.Metric.unavailable();

        BlockPos climatePosition = generator != null && generator.actualPos != null
                ? generator.actualPos : fallbackClimatePosition;
        int climateLevel = WeatherForecast.getTemperatureLevel(
                WorldTemperature.climate(generator != null && generator.actualPos != null ? towerWorld : world,
                        climatePosition));

        TownOperationalStatus status = new TownOperationalStatus(
                world.getGameTime(), residents.population(), residents.averageHealth(), residents.p10Health(),
                residents.averageMental(), residents.p10Mental(), residents.unableToWorkCount(),
                residents.exitRiskCount(), homeless, unemployed, foodReserve, fuelReserve,
                minHouseTemperature, minBuildingTemperature, unsafeHouses, stoppedHunting,
                tower, climateLevel, List.of());
        FHConfig.Server.Town.Observation observation = FHConfig.SERVER.TOWN.OBSERVATION;
        return status.withActiveAlerts(TownOperationalStatusModel.activeAlerts(status,
                observation.reserveWarningDays.get(), observation.reserveCriticalDays.get()));
    }

    private static TownOperationalStatus.TowerStatus captureTower(ServerLevel world, GeneratorData generator) {
        if (generator == null || generator.actualPos == null) return TownOperationalStatus.TowerStatus.absent();
        TownOperationalStatus.TowerKind kind = CMultiblockHelper.getBEHelperOptional(world, generator.actualPos)
                .map(helper -> {
                    Object state = helper.getState();
                    if (state instanceof T1GeneratorState) return TownOperationalStatus.TowerKind.T1;
                    if (state instanceof T2GeneratorState) return TownOperationalStatus.TowerKind.T2;
                    return TownOperationalStatus.TowerKind.UNKNOWN;
                })
                .orElse(TownOperationalStatus.TowerKind.UNKNOWN);
        double fraction = generator.getMaxOverdrive() > 0
                ? generator.overdriveLevel / (double) generator.getMaxOverdrive() : 0.0;
        return new TownOperationalStatus.TowerStatus(kind, generator.isWorking, generator.isActive,
                generator.isBroken, generator.isOverdrive, fraction);
    }

    private static TownOperationalStatus.Metric captureT1FuelReserve(
            ServerLevel world,
            com.teammoeg.chorda.dataholders.team.TeamDataHolder teamData,
            TeamTown town,
            GeneratorData generator
    ) {
        if (generator == null) return TownOperationalStatus.Metric.unavailable();
        List<GeneratorRecipe> recipes = CUtils.filterRecipes(world.getRecipeManager(), GeneratorRecipe.TYPE);
        double researchBonus = teamData.getData(FRSpecialDataTypes.RESEARCH_DATA)
                .getVariantDouble(ResearchVariant.GENERATOR_EFFICIENCY);
        double baseMultiplier = FHConfig.SERVER.TOWN.GENERATOR_T1.baseFuelDurationMultiplier.get();
        List<TownOperationalStatusModel.FuelStock> stocks = new ArrayList<>();

        ItemStack input = generator.inventory.getStackInSlot(GeneratorData.INPUT_SLOT);
        addFuelStock(stocks, input, input.getCount(), recipes, baseMultiplier, researchBonus);
        for (Map.Entry<ItemStackResourceKey, Double> entry : town.getResourceHolder().getAllItems().entrySet()) {
            addFuelStock(stocks, entry.getKey().toItemStack(), entry.getValue(), recipes,
                    baseMultiplier, researchBonus);
        }
        long totalTicks = TownOperationalStatusModel.totalProcessTicks(generator.process, stocks);
        FHConfig.Server.Town.GeneratorT1 config = FHConfig.SERVER.TOWN.GENERATOR_T1;
        return TownOperationalStatusModel.t1FuelReserveDays(totalTicks,
                config.baseProcessTicksPerGameTick.get(),
                config.overdriveExtraProcessTicksPerGameTick.get(), generator.isOverdrive, 24000);
    }

    private static void addFuelStock(
            List<TownOperationalStatusModel.FuelStock> stocks,
            ItemStack stack,
            double itemCount,
            List<GeneratorRecipe> recipes,
            double baseMultiplier,
            double researchBonus
    ) {
        if (stack.isEmpty() || itemCount <= 0.0) return;
        for (GeneratorRecipe recipe : recipes) {
            if (recipe.input.test(stack)) {
                stocks.add(new TownOperationalStatusModel.FuelStock(itemCount, recipe.input.getCount(),
                        GeneratorFuelModel.effectiveFuelProcessTicks(
                                recipe.time, baseMultiplier, researchBonus)));
                return;
            }
        }
    }
}
