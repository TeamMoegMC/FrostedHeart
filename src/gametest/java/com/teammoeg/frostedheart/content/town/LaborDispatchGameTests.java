package com.teammoeg.frostedheart.content.town;

import java.util.UUID;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.robotics.LaborTypes;
import com.teammoeg.frostedheart.content.town.buildings.dispatch.LaborDispatchBuilding;
import com.teammoeg.frostedheart.content.town.buildings.dispatch.LaborDispatchBlockEntity;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_labor_dispatch")
@PrefixGameTestTemplate(false)
public class LaborDispatchGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void supplyPersistenceAndWithdrawal(GameTestHelper helper) {
        var holder = TownBuildingGameTests.newTeam(helper, "Dispatch validation");
        var data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        var town = data.createTeamTown();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var core = TownBuildingGameTests.placeCore(helper, holder, pos, FHBlocks.LABOR_DISPATCH.get());
        helper.assertTrue(core instanceof LaborDispatchBlockEntity, "registered block entity");
        var building = (LaborDispatchBuilding) town.getTownBuilding(pos).orElseThrow();
        building.setArea(12);
        building.setVolume(24);
        building.setMaxResidents(3);
        building.setInitialized(true);
        building.setIsStructureValid(true);
        building.setConnectivityWorkable(true);
        var resident = new Resident("Dispatch", "Worker", UUID.randomUUID(), Resident.AGE_ADULT, 0);
        resident.setHealth(80);
        resident.setMental(80);
        resident.setHousePos(pos.offset(5, 0, 0));
        data.residents.put(resident.getUUID(), resident);
        building.addResident(resident);
        helper.assertTrue(building.availableWorkers(town) == 1, "one eligible worker supplies one point");
        var encoded = LaborDispatchBuilding.CODEC.encodeStart(NbtOps.INSTANCE, building).getOrThrow(false, s -> {});
        var decoded = LaborDispatchBuilding.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow(false, s -> {});
        decoded.setConnectivityWorkable(true);
        helper.assertTrue(decoded.getMaxResidents() == 3 && decoded.availableWorkers(town) == 1, "codec retains slots and roster");
        var dispatchCodec = com.teammoeg.frostedheart.content.town.building.ITownBuilding.BASE_CODEC;
        var wire = dispatchCodec.encodeStart(NbtOps.INSTANCE, building).getOrThrow(false, s -> {});
        helper.assertTrue(dispatchCodec.parse(NbtOps.INSTANCE, wire).getOrThrow(false, s -> {}) instanceof LaborDispatchBuilding,
                "town synchronization dispatch codec supports new building");
        resident.setHealth(0);
        helper.assertTrue(building.availableWorkers(town) == 0, "ineligible worker withdrawn");
        resident.setHealth(80);
        building.setIsStructureValid(false);
        helper.assertTrue(building.availableWorkers(town) == 0, "invalid building supplies nothing");
        var labor = holder.getData(FHSpecialDataTypes.LABOUR_DATA);
        labor.setExtraProviderValue(LaborTypes.WORKER, 9);
        data.tick(helper.getLevel(), holder);
        helper.assertTrue(labor.getExtraProvider(LaborTypes.WORKER) == 0, "town tick clears stale supply");
        helper.getLevel().removeBlock(pos, false);
        helper.assertTrue(town.getTownBuilding(pos).isEmpty(), "breaking core unregisters building");
        data.tick(helper.getLevel(), holder);
        helper.assertTrue(labor.getExtraProvider(LaborTypes.WORKER) == 0, "removed building does not restore supply");
        helper.succeed();
    }
}
