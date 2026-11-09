package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBlockEntity;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Scheduling regression checks plus the physical scene shared with the rendered client. */
@GameTestHolder("frostedheart_town_scheduling")
@PrefixGameTestTemplate(false)
public final class TownSchedulingGameTests {
    private TownSchedulingGameTests() {}

    @GameTest(template = "phase0a_empty", batch = "scheduling_delta", timeoutTicks = 40)
    public static void registrationDeltasSendAuthoritativeQueuesAfterRecruitment(GameTestHelper helper) {
        try (var session = new TownViewSyncGameTests.Session(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)), null)) {
            session.warm();
            populate(session);
            settle(session);
            session.drain();
            helper.assertTrue(session.firstProbe.lastStaffingPlan != null && session.firstProbe.lastHousingPlan != null,
                    "construction must send both authoritative plans over the real connection alongside building deltas");
            helper.assertTrue(session.firstProbe.lastStaffingPlan.equals(session.data.getStaffingPlan())
                            && session.firstProbe.lastHousingPlan.equals(session.data.getHousingPlan())
                            && session.firstProbe.lastStaffingPlan.target(session.extraPos) == 0,
                    "client targets must come from the server plan even if the newly delivered building already has three workers");
            helper.assertTrue(session.firstProbe.full == 0, "plan updates must not require a full town refresh");
            session.firstProbe.clear();
            ((HuntingBaseBuilding) session.data.buildings.get(session.extraPos)).setTemperature(21);
            session.data.tick(session.level, session.holder);
            session.drain();
            helper.assertTrue(session.firstProbe.buildings > 0 && session.firstProbe.staffingPlans == 0
                            && session.firstProbe.housingPlans == 0,
                    "ordinary temperature and report updates must not resend unchanged plans");
            session.place(session.extraPos, FHBlocks.HOUSE.get());
            session.data.tick(session.level, session.holder);
            session.drain();
            helper.assertTrue(session.firstProbe.lastStaffingPlan.entries().isEmpty()
                            && session.firstProbe.lastHousingPlan.entries().size() == 2,
                    "a replacement core must update both authoritative queues");
            helper.succeed();
        }
    }

    @GameTest(template = "phase0a_empty", batch = "scheduling_delta", timeoutTicks = 40)
    public static void ordinaryBuildingUpdatesRetainPlayerTargetsAndOrder(GameTestHelper helper) {
        TeamTownData client = new TeamTownData(null);
        BlockPos a = new BlockPos(1, 65, 1), b = a.offset(10, 0, 0), home = a.offset(20, 0, 0);
        client.applyBuildingUpdate(Map.of(a, new HuntingBaseBuilding(a), b, new HuntingBaseBuilding(b),
                home, new HouseBuilding(home)), Set.of());
        client.applyStaffingPlan(new TownStaffingPlan(List.of(new TownStaffingPlan.Entry(b, 2), new TownStaffingPlan.Entry(a, 1))));
        client.applyHousingPlan(new TownHousingPlan(List.of(new TownHousingPlan.Entry(home, 2))));
        TownStaffingPlan staffing = client.getStaffingPlan();
        TownHousingPlan housing = client.getHousingPlan();
        client.applyBuildingUpdate(Map.of(a, new HuntingBaseBuilding(a), b, new HuntingBaseBuilding(b),
                home, new HouseBuilding(home)), Set.of());
        helper.assertTrue(staffing.equals(client.getStaffingPlan()) && housing.equals(client.getHousingPlan()),
                "complete building records in routine deltas must not reset authored queue order or guarantees");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "scheduling_gameplay", timeoutTicks = 200)
    public static void zeroGuaranteesStillHouseEmployAndFeedThreeResidents(GameTestHelper helper) {
        try (var session = new TownViewSyncGameTests.Session(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)), null)) {
            populate(session);
            settle(session);
            helper.assertTrue(session.data.getStaffingPlan().target(session.extraPos) == 0
                            && session.data.getHousingPlan().guaranteedResidents(session.housePos) == 0,
                    "zero guarantees remain player policy after settlement");
            helper.assertTrue(session.data.residents.size() == 3 && session.data.residents.values().stream()
                            .allMatch(resident -> session.housePos.equals(resident.getHousePos())
                                    && session.extraPos.equals(resident.getWorkPos())),
                    "zero guarantees must still give all three residents actual housing and jobs");
            helper.assertTrue(((HouseBuilding) session.data.buildings.get(session.housePos)).getDailyReport().foodConsumed() > 0,
                    "the ordinary daily settlement must consume food for zero-guarantee occupants");
            helper.succeed();
        }
    }

    static void populate(TownViewSyncGameTests.Session session) {
        // Real doors, beds, walls and scanners; temperature is a controlled 20 C input to this scheduling fixture.
        buildRoom(session, session.origin, true);
        buildRoom(session, session.origin.offset(20, 0, 0), false);
        var house = (HouseBlockEntity) session.place(session.housePos, FHBlocks.HOUSE.get());
        house.refresh_safe(house.getBuilding().orElseThrow());
        var warehouse = (WarehouseBlockEntity) session.place(session.warehousePos, FHBlocks.WAREHOUSE.get());
        warehouse.refresh_safe(warehouse.getBuilding().orElseThrow());
        var hunting = (HuntingBaseBlockEntity) session.place(session.extraPos, FHBlocks.HUNTING_BASE.get());
        hunting.refresh_safe(hunting.getBuilding().orElseThrow());
        temperate(session);
        if (house.getBuilding().orElseThrow().getMaxResidents() < 3
                || hunting.getBuilding().orElseThrow().getMaxResidents() < 3)
            throw new AssertionError("scanned house and hunting room must each provide three places");
        for (int index = 1; index <= 3; index++) {
            Resident resident = new Resident("Scheduling", "Resident" + index, UUID.randomUUID());
            resident.setHealth(80);
            resident.setMental(80);
            if (!session.data.createTeamTown().addResident(resident)) throw new AssertionError("physical house must accept resident " + index);
        }
        session.data.createTeamTown().getActionExecutorHandler().execute(new TownResourceActions.ItemStackAction(
                new ItemStack(Items.BREAD, 64), ResourceActionType.ADD, ResourceActionMode.ATTEMPT));
        session.data.prepareView(session.level, session.holder);
    }

    static void temperate(TownViewSyncGameTests.Session session) {
        for (AbstractTownBuilding building : session.data.buildings.values()) {
            if (building instanceof HouseBuilding house) house.setTemperature(20);
            if (building instanceof HuntingBaseBuilding hunt) hunt.setTemperature(20);
        }
    }

    static void settle(TownViewSyncGameTests.Session session) {
        temperate(session);
        if (TownSettlement.advanceOneDay(session.data, session.level, session.holder, OptionalLong.empty())
                != TownSettlement.Result.COMMITTED) throw new AssertionError("real town day must commit");
        session.data.tick(session.level, session.holder);
    }

    static void buildRoom(TownViewSyncGameTests.Session session, BlockPos start, boolean beds) {
        for (int x = 0; x <= 6; x++) for (int y = 0; y <= 4; y++) for (int z = 0; z <= 6; z++) {
            boolean shell = x == 0 || x == 6 || y == 0 || y == 4 || z == 0 || z == 6;
            session.level.setBlockAndUpdate(start.offset(x, y, z), (shell ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        BlockPos door = start.offset(3, 1, 0);
        session.level.setBlockAndUpdate(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        session.level.setBlockAndUpdate(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        if (beds) for (int x = 1; x <= 3; x++) {
            BlockPos bed = start.offset(x, 1, 3);
            session.level.setBlockAndUpdate(bed, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
            session.level.setBlockAndUpdate(bed.south(), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        }
    }
}
