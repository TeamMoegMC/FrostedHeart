package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.model.TownAssignmentModel;
import com.teammoeg.frostedheart.content.town.model.TownResidentCareModel;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Building rosters own actual assignments; resident positions mirror them. */
final class TownAssignments {
    static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private TownAssignments() {}

    static void reconcile(TeamTownData data) {
        // Capture legacy staffing guarantees from old rosters before removing
        // stale references or duplicates. This never reallocates or feeds anyone.
        data.prepareAssignments();
        LinkedHashSet<BlockPos> ordered = new LinkedHashSet<>();
        data.getHousingPlan().entries().forEach(entry -> ordered.add(entry.building()));
        data.getStaffingPlan().entries().forEach(entry -> ordered.add(entry.building()));
        data.buildings.keySet().stream().sorted(POSITION_ORDER).forEach(ordered::add);
        Set<UUID> housed = new HashSet<>();
        Set<UUID> employed = new HashSet<>();
        var houses = new HashMap<UUID, BlockPos>();
        var jobs = new HashMap<UUID, BlockPos>();
        List<BlockPos> candidates = new ArrayList<>(ordered);
        candidates.sort(Comparator.comparing((BlockPos pos) -> data.getBuildingStore().isRestored(pos)));
        for (BlockPos pos : candidates) {
            AbstractTownBuilding building = data.buildings.get(pos);
            if (!(building instanceof ITownResidentBuilding roster)) {
                continue;
            }
            boolean housing = building instanceof HouseBuilding;
            boolean recovering = data.getBuildingStore().isRestored(pos);
            Set<UUID> seen = housing ? housed : employed;
            Set<UUID> valid = new LinkedHashSet<>();
            for (UUID id : roster.getResidentsID()) {
                Resident resident = data.residents.get(id);
                if (recovering && (!building.isBuildingWorkable() || valid.size() >= roster.getMaxResidents()
                        || resident != null && building instanceof ITownResidentWorkBuilding workplace
                        && !workplace.canResidentWork(resident))) {
                    continue;
                }
                if (data.residents.containsKey(id) && seen.add(id)) {
                    valid.add(id);
                }
            }
            roster.replaceResidents(valid);
            for (UUID id : valid) {
                if (housing) {
                    houses.put(id, pos);
                } else {
                    jobs.put(id, pos);
                }
            }
        }
        data.residents.values().forEach(resident -> {
            resident.initializeServerData();
            Resident.PendingAssignments pending = resident.getPendingAssignments();
            if (pending.housePos().isPresent() || pending.workPos().isPresent()) {
                resident.setPendingAssignments(new Resident.PendingAssignments(
                        pending.housePos().filter(pos -> !data.buildings.containsKey(pos)
                                || data.buildings.get(pos) instanceof HouseBuilding),
                        pending.workPos().filter(pos -> !data.buildings.containsKey(pos)
                                || data.buildings.get(pos) instanceof ITownResidentWorkBuilding)));
            }
            BlockPos house = houses.get(resident.getUUID());
            BlockPos work = jobs.get(resident.getUUID());
            if (house == null && resident.getHousePos() != null
                    && data.getBuildingStore().preservesReference(resident.getHousePos())) {
                resident.rememberPendingHouse(resident.getHousePos());
            } else {
                resident.setHousePos(house);
            }
            if (work == null && resident.getWorkPos() != null
                    && data.getBuildingStore().preservesReference(resident.getWorkPos())) {
                resident.rememberPendingWork(resident.getWorkPos());
            } else {
                resident.setWorkPos(work);
            }
        });
        // Existing valid rosters have priority. Recovery intent fills only an unused slot.
        for (Resident resident : data.residents.values()) {
            resident.getPendingAssignments().housePos().ifPresent(position -> {
                AbstractTownBuilding candidate = data.buildings.get(position);
                if (resident.getHousePos() == null && candidate instanceof HouseBuilding house
                        && house.isBuildingWorkable() && house.getResidentsID().size() < house.getMaxResidents()) {
                    Set<UUID> roster = new LinkedHashSet<>(house.getResidentsID());
                    roster.add(resident.getUUID());
                    house.replaceResidents(roster);
                    resident.setHousePos(position);
                }
            });
        }
        for (Resident resident : data.residents.values()) {
            resident.getPendingAssignments().workPos().ifPresent(position -> {
                AbstractTownBuilding candidate = data.buildings.get(position);
                if (resident.getWorkPos() == null && candidate instanceof ITownResidentWorkBuilding workplace
                        && candidate.isBuildingWorkable() && workplace.canResidentWork(resident)
                        && workplace.getResidentsID().size() < workplace.getMaxResidents()) {
                    Set<UUID> roster = new LinkedHashSet<>(workplace.getResidentsID());
                    roster.add(resident.getUUID());
                    workplace.replaceResidents(roster);
                    resident.setWorkPos(position);
                }
            });
        }
        data.getBuildingStore().finishRestore();
    }

    static void allocateHouse(TeamTownData data) {
        Map<UUID, BlockPos> previousHouses = new HashMap<>();
        data.residents.values().forEach(resident -> {
            if (resident.getHousePos() != null) {
                previousHouses.put(resident.getUUID(), resident.getHousePos());
            }
        });
        Map<HouseBuilding, Set<UUID>> planned = new LinkedHashMap<>();
        Map<UUID, BlockPos> finalHomes = new HashMap<>();
        FHConfig.Server.Town.ResidentRules rules = FHConfig.SERVER.TOWN.RESIDENT_RULES;
        FHConfig.Server.Town.Housing nutrition = FHConfig.SERVER.TOWN.HOUSING;
        Map<UUID, Resident> remaining = new LinkedHashMap<>();
        data.residents.values().stream().sorted(Comparator.comparing(Resident::getUUID))
                .forEach(resident -> remaining.put(resident.getUUID(), resident));
        Map<UUID, TownResidentCareModel.Need> needs = new HashMap<>();
        for (Resident resident : data.residents.values()) {
            needs.put(resident.getUUID(), TownResidentCareModel.assess(
                    resident.getUUID(), resident.getAge(), resident.getHealth(),
                    resident.getMental(), resident.getNutrition(),
                    rules.minimumWorkingAge.get(),
                    rules.minimumWorkingHealthExclusive.get(),
                    rules.minimumWorkingMentalExclusive.get(),
                    rules.removalHealthThreshold.get(),
                    rules.removalMentalThreshold.get(),
                    nutrition.residentNutritionHealthyReserve.get(),
                    nutrition.residentNutritionSevereReserve.get()));
        }
        TownCareLaw law = data.getPolicyState().careLaw();
        for (TownHousingPlan.Entry entry : data.getHousingPlan().entries()) {
            AbstractTownBuilding value = data.buildings.get(entry.building());
            if (!(value instanceof HouseBuilding house) || !house.isBuildingWorkable()) {
                continue;
            }
            Comparator<Resident> comparator = Comparator.comparing(
                    resident -> needs.get(resident.getUUID()),
                    TownResidentCareModel.comparatorForHouse(
                            law, id -> entry.building().equals(previousHouses.get(id)),
                            rules.residentialCareScoreBand.get()));
            List<Resident> selected = remaining.values().stream()
                    .sorted(comparator)
                    .limit(Math.max(0, house.getMaxResidents()))
                    .toList();
            for (Resident resident : selected) {
                planned.computeIfAbsent(house, ignored -> new LinkedHashSet<>()).add(resident.getUUID());
                finalHomes.put(resident.getUUID(), house.getPos());
                remaining.remove(resident.getUUID());
            }
        }
        for (AbstractTownBuilding value : data.buildings.values()) {
            if (value instanceof HouseBuilding house) {
                house.replaceResidents(planned.getOrDefault(house, Set.of()));
            }
        }
        data.residents.values().forEach(resident -> resident.setHousePos(finalHomes.get(resident.getUUID())));
    }

    static void allocateNewResident(TeamTownData data, Resident resident) {
        resident.setHousePos(null);
        for (TownHousingPlan.Entry entry : data.getHousingPlan().entries()) {
            AbstractTownBuilding value = data.buildings.get(entry.building());
            if (!(value instanceof HouseBuilding house) || !house.isBuildingWorkable()) {
                continue;
            }
            if (house.getResidentsID().size() >= Math.max(0, house.getMaxResidents())) {
                continue;
            }
            house.addResident(resident);
            return;
        }
    }

    static void assignWork(TeamTownData data) {
        TownStaffingPlan plan = data.getStaffingPlan();
        Map<BlockPos, ITownResidentWorkBuilding> workBuildings = new LinkedHashMap<>();
        for (TownStaffingPlan.Entry entry : plan.entries()) {
            AbstractTownBuilding building = data.buildings.get(entry.building());
            if (building instanceof ITownResidentWorkBuilding workBuilding) {
                workBuildings.put(entry.building(), workBuilding);
            }
        }

        Map<Resident, ITownResidentWorkBuilding> previous = new IdentityHashMap<>();
        for (Resident resident : data.residents.values()) {
            AbstractTownBuilding old = resident.getWorkPos() == null
                    ? null : data.buildings.get(resident.getWorkPos());
            if (old instanceof ITownResidentWorkBuilding workBuilding) {
                previous.put(resident, workBuilding);
            }
        }
        List<TownAssignmentModel.Workplace<ITownResidentWorkBuilding>> workplaces =
                new ArrayList<>();
        for (TownStaffingPlan.Entry entry : plan.entries()) {
            ITownResidentWorkBuilding building = workBuildings.get(entry.building());
            if (building != null) {
                workplaces.add(new TownAssignmentModel.Workplace<>(
                        building, building.getMaxResidents(), entry.targetWorkers(),
                        ((AbstractTownBuilding) building).isBuildingWorkable()));
            }
        }

        TownAssignmentModel.Plan<Resident, ITownResidentWorkBuilding> result =
                TownAssignmentModel.plan(
                        new ArrayList<>(data.residents.values()), workplaces,
                        previous::get,
                        ITownResidentWorkBuilding::canResidentWork,
                        ITownResidentWorkBuilding::getResidentScore,
                        Comparator.comparing(Resident::getUUID));

        Map<ITownResidentWorkBuilding, Set<UUID>> finalRosters = new LinkedHashMap<>();
        Map<UUID, BlockPos> finalJobs = new HashMap<>();
        result.assignments().forEach(assignment -> {
            finalRosters.computeIfAbsent(assignment.workplace(), ignored -> new LinkedHashSet<>()).add(assignment.resident().getUUID());
            finalJobs.put(assignment.resident().getUUID(), ((AbstractTownBuilding) assignment.workplace()).getPos());
        });
        workBuildings.values().forEach(building -> building.replaceResidents(finalRosters.getOrDefault(building, Set.of())));
        data.residents.values().forEach(resident -> resident.setWorkPos(finalJobs.get(resident.getUUID())));
        int targetShortfall = result.workplaces().values().stream()
                .mapToInt(TownAssignmentModel.WorkplaceStatus::targetShortfall)
                .sum();
        data.queueStaffingTargetCrossing(targetShortfall);
    }
}
