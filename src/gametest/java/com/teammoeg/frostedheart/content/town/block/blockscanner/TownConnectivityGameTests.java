package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Synthetic terrain runs the production graph algorithm without constructing a town or loading terrain. */
@GameTestHolder("frostedheart_town_connectivity")
@PrefixGameTestTemplate(false)
public final class TownConnectivityGameTests {
    private static final BlockPos START = new BlockPos(0, 1, 0);
    private static final class World implements AbstractWorld {
        final Map<BlockPos, BlockType> cells = new HashMap<>();
        /** Cells this world does not know, as a scan does not know unloaded chunks: read as walls. */
        final Set<BlockPos> unknown = new java.util.HashSet<>();
        public BlockType getBlockType(BlockPos pos) { return unknown.contains(pos) ? BlockType.WALL : cells.getOrDefault(pos, BlockType.WALL); }
        @Override public boolean isKnown(BlockPos pos) { return !unknown.contains(pos); }
        public int getMaxY(BlockPos pos) { return 256; }
        void air(int x, int y, int z) { cells.put(new BlockPos(x, y, z), BlockType.AIR); }
        void corridor(int end) {
            for (int x = 1; x < end; x++) { air(x, 1, 0); air(x, 2, 0); }
        }
        Set<BlockPos> solve(Set<BlockPos> buildings, BlockPos tower) {
            return TownConnectivity.calculate(buildings, tower, () -> this, () -> false);
        }
    }

    @GameTest(template = "phase0a_empty")
    public static void sixtyStepsInclusive(GameTestHelper helper) {
        World world = new World(); world.corridor(62);
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(62, 1, 0)).contains(START), "60 steps must connect");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void sixtyOneStepsExcluded(GameTestHelper helper) {
        World world = new World(); world.corridor(63);
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(63, 1, 0)).isEmpty(), "61 steps must not connect");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void relaySharesTowerConnection(GameTestHelper helper) {
        World world = new World(); world.corridor(124);
        BlockPos relay = new BlockPos(62, 1, 0);
        world.cells.remove(relay); world.cells.remove(relay.above());
        helper.assertTrue(world.solve(Set.of(START, relay), new BlockPos(124, 1, 0)).equals(Set.of(START, relay)),
                "two 60-step segments must share the tower connection");
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(124, 1, 0)).isEmpty(), "without relay the distant tower is unreachable");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void wallAndHeadClearance(GameTestHelper helper) {
        World world = new World(); world.corridor(12);
        world.cells.remove(new BlockPos(6, 2, 0));
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(12, 1, 0)).isEmpty(), "a one-block-high gap must block the route");
        world.air(6, 2, 0);
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(12, 1, 0)).contains(START), "restoring headroom must restore connection");
        world.cells.remove(new BlockPos(6, 1, 0));
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(12, 1, 0)).isEmpty(), "solid wall must block connection");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void diagonalStairDistancePruning(GameTestHelper helper) {
        World world = new World();
        for (int x = 1; x <= 61; x++) for (int dy = 0; dy <= 2; dy++) world.air(x, x + dy, 0);
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(62, 61, 0)).contains(START),
                "60 ascending steps change both X and Y; Manhattan distance must not incorrectly prune them");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void ladderAndUnsupportedAir(GameTestHelper helper) {
        World world = new World();
        for (int y = 1; y <= 20; y++) {
            world.air(1, y, 0);
            world.cells.put(new BlockPos(1, y, 0), AbstractWorld.BlockType.LADDER);
        }
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(0, 19, 0)).contains(START), "ladder must permit vertical travel");
        world.cells.replaceAll((pos, type) -> AbstractWorld.BlockType.AIR);
        helper.assertTrue(world.solve(Set.of(START), new BlockPos(0, 19, 0)).isEmpty(), "unsupported air must not permit flight");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void doorsAlwaysPassable(GameTestHelper helper) {
        LoadedTownWorld world = new LoadedTownWorld(helper.getLevel());
        BlockPos door = helper.absolutePos(new BlockPos(1, 2, 1));
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.OAK_DOOR, Blocks.IRON_DOOR}) {
            for (boolean open : new boolean[]{false, true}) for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                helper.getLevel().setBlock(door, block.defaultBlockState().setValue(DoorBlock.OPEN, open)
                        .setValue(DoorBlock.HALF, half), 2);
                helper.assertTrue(world.getPathBlockType(door) == AbstractWorld.BlockType.AIR,
                        "wood/iron doors, both halves and both open states must be passable");
            }
        }
        helper.getLevel().setBlock(door, Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(world.getPathBlockType(door) == AbstractWorld.BlockType.WALL, "ordinary blocks must keep their collision classification");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void disconnectedClusterAndNegativeCoordinates(GameTestHelper helper) {
        World world = new World();
        for (int x = -30; x < 0; x++) { world.air(x, 1, -17); world.air(x, 2, -17); }
        for (int x = -30; x < 0; x++) { world.air(x, 1, -33); world.air(x, 2, -33); }
        BlockPos connected = new BlockPos(-31, 1, -17);
        BlockPos isolated = new BlockPos(-31, 1, -33);
        helper.assertTrue(world.solve(Set.of(connected, isolated), new BlockPos(0, 1, -17)).equals(Set.of(connected)),
                "only the tower's cluster may work, including at negative coordinates");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void forestWitnessesAndPoweredFlags(GameTestHelper helper) {
        World world = new World(); world.corridor(124);
        BlockPos relay = new BlockPos(62, 1, 0), tower = new BlockPos(124, 1, 0);
        world.cells.remove(relay); world.cells.remove(relay.above());
        var view = TownConnectivity.calculateView(Set.of(START, relay), tower, () -> world, () -> false);
        helper.assertTrue(view.routes().size() == 2 && view.connected().equals(Set.of(START, relay)),
                "relay chain should transmit exactly two spanning-forest routes");
        for (var route : view.routes()) {
            helper.assertTrue(route.powered() && route.steps().size() == 61, "each powered witness must contain 60 moves");
            for (int step = 1; step < route.steps().size(); step++) {
                helper.assertTrue(RoomPathfinder.getNeighbors(world, route.from(), route.steps().get(step - 1))
                        .contains(route.steps().get(step)), "every transmitted step must be a real production neighbour");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void isolatedClusterHasUnpoweredWitness(GameTestHelper helper) {
        World world = new World(); world.corridor(12);
        var view = TownConnectivity.calculateView(Set.of(START, new BlockPos(12, 1, 0)),
                new BlockPos(200, 1, 0), () -> world, () -> false);
        helper.assertTrue(view.connected().isEmpty() && view.routes().size() == 1 && !view.routes().get(0).powered(),
                "an isolated cluster needs one amber witness and no powered buildings");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void compactRoadPacketRoundTrip(GameTestHelper helper) {
        var steps = java.util.List.of(new BlockPos(-17, -1, 16), new BlockPos(-16, 0, 16),
                new BlockPos(-16, 1, 16), new BlockPos(-16, 0, 15));
        var from = steps.get(0).west(); var to = steps.get(3).east();
        var view = new TownConnectivity.Result(Set.of(from), Set.of(from, to), java.util.List.of(
                new TownConnectivity.Route(from, to, steps, true)));
        var packet = new com.teammoeg.frostedheart.content.town.network.TownRoadViewPacket(
                java.util.UUID.randomUUID(), new net.minecraft.resources.ResourceLocation("minecraft", "overworld"),
                java.util.UUID.randomUUID(), view);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            packet.encode(buffer);
            var decoded = new com.teammoeg.frostedheart.content.town.network.TownRoadViewPacket(buffer);
            helper.assertTrue(decoded.holder.equals(packet.holder) && decoded.version.equals(packet.version)
                    && decoded.dimension.equals(packet.dimension) && decoded.view.equals(view) && !buffer.isReadable(),
                    "delta-compressed steps must round-trip negative coordinates, section boundaries, stairs and ladders");
        } finally { buffer.release(); }
        helper.succeed();
    }

    private static World splitTown() {
        World world = new World();
        for (int x = 1; x < 36; x++) { world.air(x, 1, 0); world.air(x, 2, 0); }
        for (int x = 1; x < 24; x++) { world.air(x, 1, 16); world.air(x, 2, 16); }
        for (BlockPos core : splitBuildings()) { world.cells.remove(core); world.cells.remove(core.above()); }
        return world;
    }
    private static Set<BlockPos> splitBuildings() {
        return Set.of(START, new BlockPos(12, 1, 0), new BlockPos(24, 1, 0),
                new BlockPos(0, 1, 16), new BlockPos(12, 1, 16));
    }
    private static Set<BlockPos> largestCluster() {
        return Set.of(START, new BlockPos(12, 1, 0), new BlockPos(24, 1, 0));
    }

    @GameTest(template = "phase0a_empty")
    public static void towerlessLargestClusterWorks(GameTestHelper helper) {
        World world = splitTown();
        var view = TownConnectivity.calculateView(splitBuildings(), null, () -> world, () -> false);
        helper.assertTrue(view.connected().isEmpty() && view.working().equals(largestCluster()),
                "without a tower only the three-building cluster may work, without claiming a tower connection");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void unreachableTowerFallsBackToLargest(GameTestHelper helper) {
        World world = splitTown();
        var view = TownConnectivity.calculateView(splitBuildings(), new BlockPos(500, 1, 0), () -> world, () -> false);
        helper.assertTrue(view.connected().isEmpty() && view.working().equals(largestCluster()),
                "when no cluster reaches its own tower, the largest cluster must work");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void smallerTowerClusterTakesPriority(GameTestHelper helper) {
        World world = splitTown();
        Set<BlockPos> small = Set.of(new BlockPos(0, 1, 16), new BlockPos(12, 1, 16));
        var view = TownConnectivity.calculateView(splitBuildings(), new BlockPos(24, 1, 16), () -> world, () -> false);
        helper.assertTrue(view.connected().equals(small) && view.working().equals(small),
                "the two-building tower cluster must work instead of the larger disconnected cluster");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void equalClustersChooseStableController(GameTestHelper helper) {
        World world = new World();
        BlockPos first = new BlockPos(-100, 1, 0), second = new BlockPos(100, 1, 0);
        var view = TownConnectivity.calculateView(Set.of(second, first), null, () -> world, () -> false);
        helper.assertTrue(view.working().equals(Set.of(first)) && view.connected().isEmpty(),
                "equal singleton clusters must deterministically select the first sorted controller");
        helper.assertTrue(TownConnectivity.calculateView(Set.of(), null, () -> world, () -> false).working().isEmpty(),
                "an empty towerless town must remain empty");
        helper.assertTrue(TownConnectivity.calculateView(Set.of(first), null, () -> world, () -> true).working().isEmpty(),
                "cancelled singleton scans must not select a fallback");
        helper.succeed();
    }

    private record TestTeam(java.util.UUID id) implements com.teammoeg.chorda.dataholders.team.AbstractTeam {
        public java.util.UUID getId() { return id; }
        public java.util.UUID getOwner() { return id; }
        public String getName() { return "road-owner-" + id; }
        public java.util.Collection<net.minecraft.server.level.ServerPlayer> getOnlineMembers() { return java.util.List.of(); }
    }

    @GameTest(template = "phase0a_empty")
    public static void foreignTeamTowerCannotActivateCluster(GameTestHelper helper) {
        var teams = com.teammoeg.chorda.dataholders.team.CTeamDataManager.INSTANCE;
        var own = teams.get(new TestTeam(java.util.UUID.randomUUID()));
        var foreign = teams.get(new TestTeam(java.util.UUID.randomUUID()));
        var ownGenerator = own.getData(com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes.GENERATOR_DATA);
        var foreignGenerator = foreign.getData(com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes.GENERATOR_DATA);
        ownGenerator.dimension = foreignGenerator.dimension = helper.getLevel().dimension();
        ownGenerator.actualPos = new BlockPos(24, 1, 16);
        foreignGenerator.actualPos = new BlockPos(36, 1, 0);
        var resolved = com.teammoeg.frostedheart.content.town.TeamTownData.townGenerator(own).orElseThrow();
        helper.assertTrue(resolved == ownGenerator && resolved != foreignGenerator, "resolve only the owning team's generator data");
        World world = splitTown();
        var view = TownConnectivity.calculateView(splitBuildings(), resolved.actualPos, () -> world, () -> false);
        helper.assertTrue(view.working().equals(Set.of(new BlockPos(0, 1, 16), new BlockPos(12, 1, 16)))
                && java.util.Collections.disjoint(view.connected(), largestCluster()),
                "a cluster beside the foreign tower cannot become the working tower cluster");
        // Fixture data describes ownership only; do not leave fake generator positions in live town ticks.
        ownGenerator.actualPos = foreignGenerator.actualPos = null;
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void declaredEntrancesAccountForLargeBuildingOffsets(GameTestHelper helper) {
        World world = new World();
        for (int x = 1; x <= 61; x++) for (int dy = 0; dy <= 2; dy++) world.air(x, x + dy, 0);
        BlockPos first = new BlockPos(-15, -15, 0), second = new BlockPos(77, 77, 0);
        var view = TownConnectivity.calculateView(Set.of(first, second), null, () -> world, () -> false,
                Map.of(first, java.util.List.of(new BlockPos(1, 1, 0)), second, java.util.List.of(new BlockPos(61, 61, 0))));
        helper.assertTrue(view.working().equals(Set.of(first, second)) && view.routes().size() == 1
                && view.routes().get(0).steps().size() == 61,
                "controller distance must include each building's entrance offset; no indoor route is required");
        helper.succeed();
    }

    /** Mines are remote dig sites their mine base takes over: they stay off the roads and need no road check to work. */
    @GameTest(template = "phase0a_empty")
    public static void minesStayOffTheRoads(GameTestHelper helper) {
        BlockPos housePos = new BlockPos(0, 1, 0), minePos = new BlockPos(500, -40, 500);
        var house = new com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding(housePos);
        var mine = new com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding(minePos);
        helper.assertTrue(TownConnectivity.onRoads(Map.of(housePos, house, minePos, mine)).keySet().equals(Set.of(housePos)),
                "a mine is neither an end nor a relay of the road network");
        helper.assertTrue(mine.isConnectivityWorkable() && !mine.isConnectedToTower() && !house.isConnectivityWorkable(),
                "a mine needs no road check; a house still waits for one");
        mine.setInitialized(true);
        mine.setIsStructureValid(true);
        helper.assertTrue(mine.isBuildingWorkable(), "an initialised mine works without any road");
        helper.succeed();
    }

    /** A formed tower encloses its controller; roads reach it on the ground around its base. */
    @GameTest(template = "phase0a_empty")
    public static void formedT1TowerReachableAtItsBase(GameTestHelper helper) {
        formedTowerReachableAtItsBase(helper, com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks.GENERATOR_T1);
    }

    @GameTest(template = "phase0a_empty")
    public static void formedT2TowerReachableAtItsBase(GameTestHelper helper) {
        formedTowerReachableAtItsBase(helper, com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks.GENERATOR_T2);
    }

    private static void formedTowerReachableAtItsBase(GameTestHelper helper,
            blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock structure) {
        var level = helper.getLevel();
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++)
            level.setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState());
        BlockPos origin = helper.absolutePos(new BlockPos(8, 1, 8));
        for (var block : structure.getStructure(level)) level.setBlockAndUpdate(origin.offset(block.pos()), block.state());
        helper.assertTrue(structure.createStructure(level, origin.offset(structure.getTriggerOffset()), net.minecraft.core.Direction.SOUTH,
                net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level)), "the tower must form");
        BlockPos tower = origin.offset(structure.getMasterFromOriginOffset());
        BlockPos building = helper.absolutePos(new BlockPos(2, 1, 9));
        level.setBlockAndUpdate(building, Blocks.STONE.defaultBlockState());
        LoadedTownWorld loaded = new LoadedTownWorld(level);
        AbstractWorld world = new AbstractWorld() {
            public BlockType getBlockType(BlockPos pos) { return loaded.getPathBlockType(pos); }
            public int getMaxY(BlockPos pos) { return pos.getY() + 2; }
        };
        var entrances = TownConnectivity.towerEntrances(world, tower);
        helper.assertTrue(!entrances.isEmpty() && entrances.stream().allMatch(pos -> pos.getY() == origin.getY()
                        && Math.max(Math.abs(pos.getX() - tower.getX()), Math.abs(pos.getZ() - tower.getZ())) == TownConnectivity.TOWER_RING),
                "a formed tower is entered from the floor just outside its base: " + entrances);
        var view = TownConnectivity.calculateView(Set.of(building), tower, () -> world, () -> false);
        helper.assertTrue(view.connected().equals(Set.of(building)) && view.routes().size() == 1 && view.routes().get(0).powered()
                        && entrances.contains(view.routes().get(0).steps().get(view.routes().get(0).steps().size() - 1)),
                "a building on open ground beside a formed tower must reach its base: " + view);
        // A two-high wall round the base leaves only its top, out of a one-block step's reach.
        for (BlockPos pos : entrances) for (int dy = 0; dy <= 1; dy++) level.setBlockAndUpdate(pos.above(dy), Blocks.STONE.defaultBlockState());
        LoadedTownWorld walledLoaded = new LoadedTownWorld(level);
        AbstractWorld walled = new AbstractWorld() {
            public BlockType getBlockType(BlockPos pos) { return walledLoaded.getPathBlockType(pos); }
            public int getMaxY(BlockPos pos) { return pos.getY() + 2; }
        };
        var blocked = TownConnectivity.calculateView(Set.of(building), tower, () -> walled, () -> false);
        helper.assertTrue(blocked.connected().isEmpty() && blocked.working().equals(Set.of(building)),
                "with the base walled off the tower is unreachable and the lone building works as the largest cluster: " + blocked);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void emptyAndCancelled(GameTestHelper helper) {
        World world = new World(); world.corridor(12);
        helper.assertTrue(world.solve(Set.of(), new BlockPos(12, 1, 0)).isEmpty(), "empty town must have no connected buildings");
        helper.assertTrue(TownConnectivity.calculate(Set.of(START), new BlockPos(12, 1, 0), () -> world, () -> true).isEmpty(),
                "cancelled batch must not publish connections");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void rememberedRoutesHoldOnlyWhereTerrainIsUnknown(GameTestHelper helper) {
        World world = new World();
        world.corridor(30);
        BlockPos far = new BlockPos(30, 1, 0);
        Set<BlockPos> both = Set.of(START, far);
        var seen = TownConnectivity.calculateView(both, null, () -> world, () -> false, Map.of(), java.util.List.of());
        helper.assertTrue(seen.routes().size() == 1 && seen.working().equals(both), "the corridor links both ends");
        for (int x = 10; x < 20; x++) for (int y = 0; y <= 2; y++) world.unknown.add(new BlockPos(x, y, 0));
        var blind = TownConnectivity.calculateView(both, null, () -> world, () -> false, Map.of(), java.util.List.of());
        helper.assertTrue(blind.routes().isEmpty(), "without a remembered route, unknown terrain cuts the corridor");
        var remembered = TownConnectivity.calculateView(both, null, () -> world, () -> false, Map.of(), seen.routes());
        helper.assertTrue(remembered.routes().equals(seen.routes()) && remembered.working().equals(both),
                "the remembered route holds through terrain the world does not know");
        world.cells.put(new BlockPos(5, 1, 0), AbstractWorld.BlockType.WALL);
        var broken = TownConnectivity.calculateView(both, null, () -> world, () -> false, Map.of(), seen.routes());
        helper.assertTrue(broken.routes().isEmpty() && broken.working().size() == 1,
                "a remembered route breaks where its terrain is known and blocked");
        helper.succeed();
    }
}
