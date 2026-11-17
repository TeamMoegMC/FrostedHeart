package com.teammoeg.frostedheart.content.town.citizen.nav;

import java.util.*;
import com.teammoeg.frostedheart.content.town.block.blockscanner.*;
import com.teammoeg.frostedheart.content.town.citizen.sim.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Shared routes per town, runtime cursors per stable citizen id. Never persists route indices. */
public final class TownRoadNavigation {
    private static final class Cursor {
        List<TownRoadNetwork.Waypoint> path = List.of();
        int next, targetX, targetZ;
        byte state;
        boolean waiting;
    }
    private final Map<Integer, Cursor> cursors = new HashMap<>();
    private UUID version;
    private TownRoadNetwork network;
    private LoadedTownWorld terrain;
    private long terrainTick = Long.MIN_VALUE;
    public void update(TownConnectivity connectivity) {
        update(connectivity.version(), connectivity.view());
    }
    public void clear() { version = null; network = null; cursors.clear(); terrain = null; terrainTick = Long.MIN_VALUE; }
    public void update(UUID nextVersion, TownConnectivity.Result view) {
        if (!nextVersion.equals(version)) {
            version = nextVersion; network = new TownRoadNetwork(view); cursors.clear();
        }
    }
    public void remove(int id) { cursors.remove(id); }
    public boolean arrived(CitizenSim sim, int i) {
        Cursor cursor = cursors.get(sim.id[i]);
        return cursor != null && !cursor.waiting && cursor.next >= cursor.path.size()
                && cursor.state == sim.state[i] && cursor.targetX == sim.tx[i] && cursor.targetZ == sim.tz[i];
    }
    public void tick(CitizenSimScheduler scheduler, CitizenContainer container, CitizenSim sim,
            ServerLevel level, int i, long time) {
        if (terrainTick != time) { terrain = new LoadedTownWorld(level); terrainTick = time; }
        BlockPos home = BlockPos.of(sim.homePos[i]);
        if (sim.homePos[i] == CitizenSim.NO_HOME_POS) home = new BlockPos(sim.homeX[i], sim.py[i] >> 10, sim.homeZ[i]);
        if (!network.working(home)) { sim.halt[i] = 1; cursors.remove(sim.id[i]); return; }
        if (!CitizenState.MOVING[sim.state[i]]) {
            if (!cursors.containsKey(sim.id[i])) {
                BlockPos entry = network.entrance(home, BlockPos.of(container.homeEntrancePosition(i)));
                if (terrain.getPathBlockType(entry) != AbstractWorld.BlockType.WALL) {
                    sim.px[i] = (entry.getX() << 10) + 512; sim.py[i] = entry.getY() << 10;
                    sim.pz[i] = (entry.getZ() << 10) + 512;
                    cursors.put(sim.id[i], new Cursor()); scheduler.sync.notifyImmediate(sim.id[i]);
                }
            }
            sim.halt[i] = 1; return;
        }
        Cursor cursor = cursors.get(sim.id[i]);
        if (cursor == null || cursor.state != sim.state[i] || cursor.targetX != sim.tx[i] || cursor.targetZ != sim.tz[i]) {
            cursor = new Cursor(); cursor.state = sim.state[i];
            BlockPos goal = switch (sim.state[i]) {
                case CitizenState.WORK -> container.workBuildingPosition(i);
                case CitizenState.RETURN_HOME -> home;
                default -> network.wanderTarget(home, CitizenState.nextRand(sim.id[i], time));
            };
            BlockPos position = new BlockPos(sim.px[i] >> 10, sim.py[i] >> 10, sim.pz[i] >> 10);
            if (goal != null) cursor.path = network.fromPosition(position, home, goal,
                    BlockPos.of(container.homeEntrancePosition(i)));
            cursor.waiting = cursor.path.isEmpty();
            if (!cursor.waiting) {
                BlockPos last = cursor.path.get(cursor.path.size() - 1).pos();
                sim.tx[i] = (last.getX() << 10) + 512; sim.tz[i] = (last.getZ() << 10) + 512;
            }
            cursor.targetX = sim.tx[i]; cursor.targetZ = sim.tz[i]; cursors.put(sim.id[i], cursor);
        }
        if (cursor.waiting || cursor.next >= cursor.path.size()) { sim.halt[i] = 1; return; }
        var waypoint = cursor.path.get(cursor.next);
        BlockPos next = waypoint.pos();
        AbstractWorld pathWorld = new AbstractWorld() {
            public BlockType getBlockType(BlockPos pos) { return terrain.getPathBlockType(pos); }
            public int getMaxY(BlockPos pos) { return terrain.getMaxY(pos); }
        };
        if (!RoomPathfinder.canStandAt(pathWorld, next)) { sim.halt[i] = 1; return; }
        int x = (next.getX() << 10) + 512, y = next.getY() << 10, z = (next.getZ() << 10) + 512;
        int dx = x - sim.px[i], dy = y - sim.py[i], dz = z - sim.pz[i];
        double distance = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
        int speed = CitizenState.SPEED[sim.state[i]];
        boolean transfer = waypoint.buildingTransfer();
        // Each segment starts at a building port. Entrance-to-exit transfer is intentionally not indoor navigation.
        if (transfer || distance <= speed) {
            sim.px[i] = x; sim.py[i] = y; sim.pz[i] = z; cursor.next++;
            if (transfer) scheduler.sync.notifyImmediate(sim.id[i]);
        } else {
            sim.px[i] += (int) Math.round(dx * speed / distance);
            sim.py[i] += (int) Math.round(dy * speed / distance);
            sim.pz[i] += (int) Math.round(dz * speed / distance);
        }
        if (dx != 0 || dz != 0) {
            byte direction = (byte) CitizenState.dirFromVector(dx, dz);
            if (direction != sim.dir[i]) scheduler.sync.notifyImmediate(sim.id[i]);
            sim.dir[i] = direction;
        }
        sim.halt[i] = (byte) ((dx == 0 && dz == 0) || transfer ? 1 : 0);
        sim.sepX[i] = sim.sepZ[i] = 0;
    }
}
