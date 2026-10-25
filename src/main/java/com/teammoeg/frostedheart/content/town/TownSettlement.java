package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WeatherForecast;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.town.TeamTownData.RefugeeSpawnWeather;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.TownHousingMealService;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentActivity;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeChange;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotificationModel;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Town.RefugeeSpawn;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Town.ResidentAging;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Ordered economic day. Scheduling and I/O are deliberately outside this boundary. */
public final class TownSettlement {
    public enum Result { COMMITTED, SKIPPED, IN_PROGRESS, FAILED }
    private TownSettlement() {}

    public static Result advanceOneDay(TeamTownData data, ServerLevel level, TeamDataHolder holder,
                                       OptionalLong automaticWindow) {
        if (data.inSettlement) return Result.IN_PROGRESS;
        if (!data.isCurrentExecutionContext(level, holder)) return Result.SKIPPED;
        if (!FHConfig.SERVER.TOWN.enableTownTickMorning.get()) return Result.SKIPPED;
        data.inSettlement = true;
        String stage = "prepare";
        boolean committed = false;
        try {
            TeamTown town = data.createTeamTown();
            data.checkBlocks(level, town);
            data.checkOccupiedAreaOverlap();
            data.activatePendingPolicies();
            TownHousingMealService.decayNutrition(data.residents.values());

            stage = "assignments";
            TownAssignments.reconcile(data);
            data.allocateHouse();
            data.assignWork();
            tickRefugeeSpawnAndDespawn(data, level, holder);
            linkMinesToBases(data);
            recalcOreChunkResources(data);
            data.residents.values().forEach(Resident::resetDailyProficiencyGrowth);

            stage = "production and meals";
            buildingsWork(data, level);
            TownHousingMealService.settle(level, town, data.getHousingPlan(),
                    data.getPolicyState().careLaw(), data.nextTownDay());

            stage = "residents and recovery";
            tickResidentsAging(data);
            tickResidentsMorning(data);
            data.recoverResources();

            stage = "prepare day commit";
            TeamTownData.PreparedDayCommit prepared = data.prepareDayCommit(level, holder);
            data.commitDay(prepared, automaticWindow);
            committed = true;

            stage = "publish day effects";
            data.publishDayEffects(prepared, holder);
            return Result.COMMITTED;
        } catch (RuntimeException failure) {
            data.needsRuntimeRepair = true;
            TownSync.invalidate(holder);
            FHMain.LOGGER.error("Town {} failed during {}; committed={}; automaticWindow={}",
                    holder.getId(), stage, committed, automaticWindow, failure);
            // Partial business facts are preserved. Repair only structure/bindings;
            // the scheduler's attempted window forbids a second economic run.
            return committed ? Result.COMMITTED : Result.FAILED;
        } finally {
            data.dayEffects.clear();
            data.inSettlement = false;
            TownRuntimeManager runtime = TownRuntimeManager.current();
            if (runtime != null) runtime.finishOperations();
        }
    }

    static void tickResidentsMorning(TeamTownData data) {
        if (ITown.DEBUG_MODE) {
            return;// 测试时村民不死
        }
        FHConfig.Server.Town.ResidentRules config = FHConfig.SERVER.TOWN.RESIDENT_RULES;
        List<Resident> deadResidents = new ArrayList<>();
        for (Resident resident : data.residents.values()) {
            ResidentDailyModel.MorningResult result = ResidentDailyModel.settleMorning(
                    resident.getHealth(),
                    resident.getMental(),
                    resident.getHousePos() != null,
                    config.homelessHealthLossPerDay.get(),
                    config.removalHealthThreshold.get(),
                    config.removalMentalThreshold.get());
            resident.setHealth(result.healthAfterHomelessPenalty());
            if (result.removed()) {
                deadResidents.add(resident);
                TownSignalEvent.Type type = result.removedForHealth() && result.removedForMental()
                        ? TownSignalEvent.Type.RESIDENT_EXIT_BOTH
                        : result.removedForHealth()
                        ? TownSignalEvent.Type.RESIDENT_EXIT_HEALTH
                        : TownSignalEvent.Type.RESIDENT_EXIT_MENTAL;
                data.pendingDailySignals.add(new TownSignalEvent(
                        -1L, 0, type, TownSignalEvent.Severity.IRREVERSIBLE, 1,
                        resident.getUUID().toString()));
            }
        }
        TeamTown town = TeamTown.create(data);
        deadResidents.forEach(resident -> resident.setDeath(town));
    }

    static void tickResidentsAging(TeamTownData data) {
        ResidentAging aging = FHConfig.SERVER.TOWN.RESIDENT_AGING;
        FHConfig.Server.Town.Housing nutritionConfig = FHConfig.SERVER.TOWN.HOUSING;
        ResidentNutritionSupportModel.Weights weights = Resident.nutritionSupportWeights();
        for (Resident resident : data.residents.values()) {
            int growthAge = resident.getAge();
            int nextAgeDays = Math.max(0, resident.getAgeDays()) + 1;
            int nextAge = growthAge;
            if (growthAge == Resident.AGE_INFANT
                    && nextAgeDays >= aging.infantToChildDays.get()) {
                nextAge = Resident.AGE_CHILD;
            } else if (growthAge == Resident.AGE_CHILD
                    && nextAgeDays >= aging.childToAdultDays.get()) {
                nextAge = Resident.AGE_ADULT;
            }

            double baseActivity;
            double strengthRate;
            double intelligenceRate;
            double strengthAgeCap;
            double intelligenceAgeCap;
            double strengthAgeDecay = 0.0;
            double intelligenceAgeDecay = 0.0;
            switch (growthAge) {
                case Resident.AGE_INFANT -> {
                    baseActivity = aging.infantBaseActivity.get();
                    strengthRate = aging.infantStrengthGainPerDay.get();
                    intelligenceRate = aging.infantIntelligenceGainPerDay.get();
                    strengthAgeCap = aging.infantAttributeCap.get();
                    intelligenceAgeCap = aging.infantAttributeCap.get();
                }
                case Resident.AGE_CHILD -> {
                    baseActivity = aging.childBaseActivity.get();
                    strengthRate = aging.childStrengthGainPerDay.get();
                    intelligenceRate = aging.childIntelligenceGainPerDay.get();
                    strengthAgeCap = aging.childStrengthCap.get();
                    intelligenceAgeCap = aging.childIntelligenceCap.get();
                }
                case Resident.AGE_ADULT -> {
                    baseActivity = aging.adultBaseActivity.get();
                    strengthRate = aging.adultStrengthGainPerDay.get();
                    intelligenceRate = aging.adultIntelligenceGainPerDay.get();
                    strengthAgeCap = aging.adultAttributeCap.get();
                    intelligenceAgeCap = aging.adultAttributeCap.get();
                }
                case Resident.AGE_ELDER -> {
                    baseActivity = aging.elderBaseActivity.get();
                    strengthRate = aging.elderStrengthGainPerDay.get();
                    intelligenceRate = aging.elderIntelligenceGainPerDay.get();
                    strengthAgeCap = 100.0;
                    intelligenceAgeCap = 100.0;
                    strengthAgeDecay = aging.elderStrengthAgeDecayPerDay.get();
                    intelligenceAgeDecay = aging.elderIntelligenceAgeDecayPerDay.get();
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported resident age: " + growthAge);
            }

            ResidentNutritionSupportModel.Supports support =
                    ResidentNutritionSupportModel.supports(
                            ResidentNutritionSupportModel.satisfaction(
                                    resident.getNutrition(),
                                    nutritionConfig.residentNutritionHealthyReserve.get()),
                            weights);
            ResidentActivity activity = resident.getDailyActivity();
            ResidentAttributeChange strengthChange = ResidentAttributeModel.settleDailyAttribute(
                    resident.getStrength(), activity.physical(), baseActivity, support.strength(),
                    strengthRate, strengthAgeCap,
                    nutritionConfig.residentStrengthGrowthEfficiencyAtZeroSupport.get(),
                    nutritionConfig.residentStrengthMaintenanceThreshold.get(),
                    nutritionConfig.residentNutritionDeficiencyExponent.get(),
                    nutritionConfig.residentStrengthDecayAtZeroSupport.get(), strengthAgeDecay);
            ResidentAttributeChange intelligenceChange = ResidentAttributeModel.settleDailyAttribute(
                    resident.getIntelligence(), activity.learning(), baseActivity, support.intelligence(),
                    intelligenceRate, intelligenceAgeCap,
                    nutritionConfig.residentIntelligenceGrowthEfficiencyAtZeroSupport.get(),
                    nutritionConfig.residentIntelligenceMaintenanceThreshold.get(),
                    nutritionConfig.residentNutritionDeficiencyExponent.get(),
                    nutritionConfig.residentIntelligenceDecayAtZeroSupport.get(), intelligenceAgeDecay);
            resident.setStrength(strengthChange.nextValue());
            resident.setIntelligence(intelligenceChange.nextValue());
            resident.setAgeDays(nextAgeDays);
            resident.setAge(nextAge);
            resident.recordNutritionAttributes(activity, strengthChange, intelligenceChange);
        }
    }

    static void tickRefugeeSpawnAndDespawn(TeamTownData data, ServerLevel world, TeamDataHolder teamData) {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        if (!config.enableRefugeeSpawn.get()) return;
        // 队伍无人在线时不刷新；不置位当天标记，有人上线当天仍可刷。
        // 防御：getTeam() 可为 null（旧存档恢复已解散队伍的 holder），getOnlineMembers() 内部不判空
        if (teamData.getTeam() == null || teamData.getTeam().getOnlineMembers().isEmpty()) return;
        // WorldClockSource 已在本次城镇 tick 前更新：睡觉跳时会推进日期，/time set 回退不会让日期倒退。
        long day = WorldClimate.getWorldDay(world);
        // "当天只结算一次"守卫：/town tick 同日多次调用也不会重复刷批
        if (day == data.lastRefugeeSpawnDay) return;
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isEmpty() || genDataOpt.get().actualPos == null || !genDataOpt.get().isWorking) {
            // 塔不存在/未开启：置位当天，防止同日反复调用重复判定
            data.lastRefugeeSpawnDay = day;
            data.persistentChanged();
            return;
        }
        GeneratorData genData = genDataOpt.get();
        BlockPos towerPos = genData.actualPos;
        RefugeeSpawnWeather weather = getSpawnWeather(data, world, towerPos);
        double chance = config.baseSpawnChancePerDay.get()
            + (weather == RefugeeSpawnWeather.WARM ? config.warmSpawnChanceBonus.get()
            : weather == RefugeeSpawnWeather.COLD ? -config.coldSpawnChancePenalty.get() : 0);
        chance = Math.max(0.0, Math.min(1.0, chance));
        if (CMath.RANDOM.nextDouble() >= chance) {
            FHMain.LOGGER.debug("No refugee batch this morning, weather={}, chance={}", weather, chance);
            data.lastRefugeeSpawnDay = day;
            data.persistentChanged();
            return;
        }
        FHMain.LOGGER.debug("Spawning refugee batch, weather={}", weather);
        int spawned = spawnRefugeeBatch(data, world, teamData, weather);
        // 全部生成失败时不置位，当天可重试
        if (spawned > 0) {
            data.lastRefugeeSpawnDay = day;
            data.persistentChanged();
        }
    }

    static void debugSpawnRefugeeBatch(TeamTownData data, ServerLevel world, TeamDataHolder teamData) {
        if (!data.isCurrentExecutionContext(world, teamData)) return;
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isEmpty() || genDataOpt.get().actualPos == null) return;
        RefugeeSpawnWeather weather = getSpawnWeather(data, world, genDataOpt.get().actualPos);
        int spawned = spawnRefugeeBatch(data, world, teamData, weather);
        if (spawned > 0) {
            data.lastRefugeeSpawnDay = WorldClimate.getWorldDay(world);
            data.persistentChanged();
        }
        FHMain.LOGGER.info("Debug-spawned {} refugee(s), weather={}", spawned, weather);
    }

    static int spawnRefugeeBatch(TeamTownData data, ServerLevel world, TeamDataHolder teamData, RefugeeSpawnWeather weather) {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isEmpty() || genDataOpt.get().actualPos == null) return 0;
        GeneratorData genData = genDataOpt.get();
        int sizeMod = weather == RefugeeSpawnWeather.WARM ? config.warmSpawnBatchBonus.get()
            : weather == RefugeeSpawnWeather.COLD ? -config.coldSpawnBatchPenalty.get() : 0;
        int min = Math.max(1, config.batchSizeMin.get() + sizeMod);
        int max = Math.max(min, config.batchSizeMax.get() + sizeMod);
        int batchSize = min + CMath.RANDOM.nextInt(max - min + 1);
        int spawned = 0;
        for (int i = 0; i < batchSize; i++) {
            BlockPos pos = findRefugeeSpawnPos(data, world, genData.actualPos);
            if (pos == null) continue;
            WanderingRefugee refugee = FHEntityTypes.WANDERING_REFUGEE.get().create(world);
            if (refugee == null) continue;
            refugee.initializeRandomGeneration();
            refugee.markTownSpawned(teamData.getId());
            if (weather == RefugeeSpawnWeather.COLD && CMath.RANDOM.nextDouble() < config.coldQualityChance.get()) {
                refugee.setColdSurvivor(true);
            }
            refugee.setPersistenceRequired();
            refugee.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            // 生成可能被世界拒绝（如实体数量上限），成功才计数并寻路，避免计数虚高使"全部失败也置位当天"
            if (world.addFreshEntity(refugee)) {
                // 默默寻路走到能量塔旁，聚拢在塔附近等待（无任何特效）
                refugee.getNavigation().moveTo(genData.actualPos.getX() + 0.5D, genData.actualPos.getY(), genData.actualPos.getZ() + 0.5D, 1.0D);
                spawned++;
            }
        }
        return spawned;
    }

    static RefugeeSpawnWeather getSpawnWeather(TeamTownData data, ServerLevel world, BlockPos towerPos) {
        int tempLevel = WeatherForecast.getTemperatureLevel(WorldTemperature.climate(world, towerPos));
        if (tempLevel >= 1 && WorldClimate.isSun(world)) {
            return RefugeeSpawnWeather.WARM;
        }
        if (tempLevel <= -1 || WorldClimate.isBlizzard(world)) {
            return RefugeeSpawnWeather.COLD;
        }
        return RefugeeSpawnWeather.NORMAL;
    }

    static BlockPos findRefugeeSpawnPos(TeamTownData data, ServerLevel world, BlockPos towerPos) {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        int minR = config.spawnRadiusMinBlocks.get();
        int maxR = Math.max(minR, config.spawnRadiusMaxBlocks.get());
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = CMath.RANDOM.nextDouble() * Math.PI * 2.0D;
            double dist = minR + CMath.RANDOM.nextDouble() * (maxR - minR);
            int x = towerPos.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = towerPos.getZ() + (int) Math.round(Math.sin(angle) * dist);
            // 未加载区块直接跳过：getHeight 对未加载区块会同步加载，塔区块卸载时每天最多 16×batchSize 次
            if (!world.hasChunkAt(x, z)) continue;
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = world.getBlockState(pos.below());
            if (!ground.isSolid()) continue;
            BlockState standing = world.getBlockState(pos);
            BlockState overhead = world.getBlockState(pos.above());
            if (!standing.isAir() && !standing.canBeReplaced()) continue;
            if (!overhead.isAir() && !overhead.canBeReplaced()) continue;
            return pos;
        }
        return null;
    }

    static void linkMinesToBases(TeamTownData data) {
        Set<BlockPos> unassigned = new HashSet<>();
        for (AbstractTownBuilding building : data.buildings.values()) {
            if (building instanceof MineBuilding mine && mine.isStructureValid()) {
                unassigned.add(mine.getPos());
            }
        }
        var bases = data.buildings.values().stream()
                .filter(building -> building instanceof MineBaseBuilding)
                .map(building -> (MineBaseBuilding) building)
                .sorted(Comparator.comparing(MineBaseBuilding::getPos, TownAssignments.POSITION_ORDER))
                .toList();
        for (MineBaseBuilding base : bases) {
            Set<BlockPos> assigned = new HashSet<>();
            if (base.isStructureValid()) {
                int radius = base.getConnectionRadius();
                for (BlockPos mine : unassigned) {
                    if (mine.distSqr(base.getPos()) <= (double) radius * radius) {
                        assigned.add(mine);
                    }
                }
            }
            unassigned.removeAll(assigned);
            base.replaceLinkedMines(assigned);
        }
    }

    static void recalcOreChunkResources(TeamTownData data) {
        Set<ChunkPos> covered = new HashSet<>();
        for (AbstractTownBuilding b : data.buildings.values()) {
            if (b instanceof MineBaseBuilding base && base.isBuildingWorkable()) {
                for (BlockPos minePos : base.getLinkedMines()) {
                    covered.add(new ChunkPos(minePos));
                }
            }
        }
        data.setTerrainResourceTypeActiveChunks(TerrainResourceType.ORE, covered);
    }

    static void buildingsWork(TeamTownData data, ServerLevel world){
        data.updateRadius();
        //updateAllBlocks(world);

        TeamTown teamTown = new TeamTown(data);
        data.resources.resetAllServices();
        data.reloadMaxCapacity();

        data.buildings.values().stream()
                .filter(AbstractTownBuilding::shouldRunDailySettlement)
                .filter(building -> !(building instanceof HouseBuilding))
                .sorted(Comparator.comparingInt(AbstractTownBuilding::getWorkPriority).reversed()
                        .thenComparing(AbstractTownBuilding::getPos, TownAssignments.POSITION_ORDER))
                .forEach(building -> building.work(teamTown,world));
        finishDailyTransportSettlement(data);
    }

    static void finishDailyTransportSettlement(TeamTownData data) {
        double totalCapacity = data.resources.get(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0));
        double reservedCapacity = data.getTransportState().getReservedTransportCapacity();
        TownTransportState.DailyReport report = new TownTransportState.DailyReport(
                true, totalCapacity, reservedCapacity);
        if (data.getTransportState().setDailyReport(report)) {
            data.dataSyncCache.markTransportStateChanged();
        }
        TownTransportShortageNotificationModel.Result shortage =
                TownTransportShortageNotificationModel.onMorningSettlement(
                        data.transportShortageTipState,
                        data.nextTownDay(),
                        totalCapacity,
                        reservedCapacity);
        data.transportShortageTipState = shortage.state();
        shortage.notice().ifPresent(data.pendingTransportShortageTips::add);
    }
}
