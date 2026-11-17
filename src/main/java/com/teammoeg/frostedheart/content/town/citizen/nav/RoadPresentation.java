package com.teammoeg.frostedheart.content.town.citizen.nav;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

/** Local client-only placement rules. No world mutation or authoritative position updates. */
public final class RoadPresentation {
    public record Pose(double x, double y, double z, int ladderYaw) { public boolean climbing() { return ladderYaw >= 0; } }
    private RoadPresentation() {}
    public static Pose resolve(BlockGetter world, int id, double x, double y, double z, double dx, double dz) {
        BlockPos foot = BlockPos.containing(x, y + .05, z);
        var state = world.getBlockState(foot);
        if (state.getBlock() instanceof LadderBlock || world.getBlockState(foot.above()).getBlock() instanceof LadderBlock) {
            var ladder = state.getBlock() instanceof LadderBlock ? state : world.getBlockState(foot.above());
            Direction facing = ladder.getValue(LadderBlock.FACING);
            // Put the torso close enough to reach the ladder, still outside its supporting block.
            double cx = foot.getX() + .5 - facing.getStepX() * .14;
            double cz = foot.getZ() + .5 - facing.getStepZ() * .14;
            int yaw = Math.floorMod(Math.round(facing.getOpposite().toYRot() * 256 / 360), 256);
            return new Pose(cx, y, cz, yaw);
        }
        boolean narrow = state.getBlock() instanceof DoorBlock || isStep(world, foot) || isStep(world, foot.below());
        double length = Math.hypot(dx, dz);
        double side = stableSide(id);
        double ox = narrow || length < .001 ? 0 : -dz / length * side;
        double oz = narrow || length < .001 ? 0 : dx / length * side;
        for (int sample = 0; sample <= 4; sample++) {
            double scale = 1 - sample / 4.0;
            double nx = x + ox * scale, nz = z + oz * scale;
            Double height = surface(world, nx, y, nz);
            if (height == null) continue;
            if (!narrow) {
                boolean supported = true;
                for (double sx : new double[]{-.27, .27}) for (double sz : new double[]{-.27, .27}) {
                    Double corner = surface(world, nx + sx, y, nz + sz);
                    if (corner == null || Math.abs(corner - height) > .01) supported = false;
                }
                if (!supported) continue;
            }
            boolean clear = true;
            // Validate the small sweep too: a free point on the other side of a wall is not a valid offset.
            for (int step = 1; step <= 4; step++) {
                double t = step / 4.0;
                if (!clearBody(world, x + (nx - x) * t, y + (height - y) * t, z + (nz - z) * t)) { clear = false; break; }
            }
            if (clear) return new Pose(nx, height, nz, -1);
        }
        return new Pose(x, y, z, -1);
    }
    public static double stableSide(int id) {
        int hash = id * 0x9E3779B9; hash ^= hash >>> 16;
        return ((hash & 1023) / 1023.0 * 2 - 1) * .9;
    }
    private static boolean isStep(BlockGetter world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return block instanceof StairBlock || block instanceof SlabBlock;
    }
    public static Double surface(BlockGetter world, double x, double y, double z) {
        Double best = null;
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        for (int fy = (int) Math.floor(y) - 2; fy <= (int) Math.floor(y); fy++) {
            BlockPos pos = new BlockPos(bx, fy, bz);
            var state = world.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || state.getBlock() instanceof DoorBlock) continue;
            for (AABB box : state.getCollisionShape(world, pos).toAabbs()) {
                double top = fy + box.maxY;
                if (x - bx >= box.minX && x - bx <= box.maxX && z - bz >= box.minZ && z - bz <= box.maxZ
                        && top <= y + .51 && top >= y - 1.01 && (best == null || Math.abs(top - y) < Math.abs(best - y))) best = top;
            }
        }
        return best;
    }
    public static boolean clearBody(BlockGetter world, double x, double y, double z) {
        AABB body = new AABB(x - .29, y + .02, z - .29, x + .29, y + 1.78, z + .29);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY, body.minZ),
                BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
            var state = world.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return false;
            for (AABB box : state.getCollisionShape(world, pos).toAabbs()) if (body.intersects(box.move(pos))) return false;
        }
        return true;
    }
}
