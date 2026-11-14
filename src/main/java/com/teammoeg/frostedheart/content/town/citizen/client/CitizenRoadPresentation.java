package com.teammoeg.frostedheart.content.town.citizen.client;

import java.util.*;
import com.teammoeg.frostedheart.content.town.citizen.nav.RoadPresentation;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;
import com.teammoeg.frostedheart.content.town.render.TownRoadRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Evaluated once per client tick, shared by detailed entities, CPU batches and GPU snapshots. */
final class CitizenRoadPresentation {
    private record Key(int id, BlockPos cell, int direction) {}
    private static final Map<Key, RoadPresentation.Pose> CACHE = new LinkedHashMap<>(256, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Key, RoadPresentation.Pose> entry) { return size() > 4096; }
    };
    private static Object lastLevel;
    private static long cacheTick;
    static void clear() { CACHE.clear(); lastLevel = null; cacheTick = 0; }
    static void tick(Minecraft mc) {
        if (lastLevel != mc.level || mc.level == null || mc.level.getGameTime() - cacheTick >= 10) {
            CACHE.clear(); lastLevel = mc.level; cacheTick = mc.level == null ? 0 : mc.level.getGameTime();
        }
        if (mc.level == null) return;
        double now = ClientCitizen.currentTimeSeconds();
        for (ClientCitizen citizen : ClientCitizenCache.values()) {
            double[] raw = citizen.renderPos(now);
            BlockPos base = BlockPos.containing(raw[0], raw[1], raw[2]);
            if (citizen.state == CitizenState.SLEEP || citizen.roadTown == null
                    || !TownRoadRenderer.matches(citizen.roadTown) || !TownRoadRenderer.nearRoad(base)
                    || !mc.level.hasChunkAt(base) || (mc.player != null && base.distSqr(mc.player.blockPosition()) > 128 * 128)) {
                citizen.roadPoseActive = false; citizen.roadClimbing = false; continue;
            }
            Key key = new Key(citizen.id, base, citizen.dir);
            // Cache only the preferred offset, re-evaluate feet height at the interpolated X/Z for stairs.
            var pose = CACHE.computeIfAbsent(key, ignored -> RoadPresentation.resolve(mc.level, citizen.id,
                    base.getX() + .5, raw[1], base.getZ() + .5,
                    CitizenState.DIR_X_16[citizen.dir], CitizenState.DIR_Z_16[citizen.dir]));
            double px = raw[0] + pose.x() - (base.getX() + .5), pz = raw[2] + pose.z() - (base.getZ() + .5);
            double py = raw[1];
            if (!pose.climbing()) {
                Double surface = RoadPresentation.surface(mc.level, px, py, pz);
                if (surface != null && RoadPresentation.clearBody(mc.level, px, surface, pz)) py = surface;
                else { px = raw[0]; pz = raw[2]; }
            } else { px = pose.x(); pz = pose.z(); }
            citizen.updateRoadPose(px, py, pz, pose.ladderYaw(), now);
        }
    }
}
