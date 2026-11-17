package com.teammoeg.frostedheart.content.world.features.deposit;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

/** Immutable world-seeded geometry shared by terrain, biome ownership and placement. */
public final class DepositLayout {
    private static final double[][] ROOMS = {
            {0, 0, 34, 18, 0, 23}, {-46, -8, 14, 9, .5, 13},
            {54, 22, 13, 9, -1, 13}, {-12, -26, 11, 6, 1, 10}
    };
    private static final double[][] SPINE = {
            {-87, -11}, {-64, -9}, {-46, -8}, {-27, -2}, {27, 3}, {44, 18}, {62, 22}, {87, 18}
    };
    private record Pool(double u, double v, double radiusU, double radiusV, double level, double depth) {}
    private static final Pool[] POOLS = {
            new Pool(-5, 5, 17, 9, -1, 2.2), new Pool(-18, -3, 8, 6, -1, 1.5),
            new Pool(22, 8, 6, 5, 0, 1.8), new Pool(-46, -9, 7.5, 4.5, -1, 1.6),
            new Pool(54, 23, 6.5, 4.5, -2, 1.8)
    };
    /** Attached solids, not surface speckles. Local positions/heights are in blocks relative to the bank. */
    private record RidgeRock(double u, double v, double halfU, double halfV, double top, double tiltU, double tiltV) {}
    private static final RidgeRock[] RIDGE_ROCKS = {
            new RidgeRock(-16, 2, 4.0, 5.0, 7.5, -.10, .12),
            new RidgeRock(-12, 10, 5.0, 6.0, 11.5, .18, -.05),
            new RidgeRock(-8, 19, 6.0, 8.0, 14.0, -.08, .14)
    };
    private final BlockPos center;
    private final double rotation, cosine, sine;
    private final long seed;
    private final AtomicReferenceArray<Entry> columns = new AtomicReferenceArray<>(256);
    private record Entry(long key, Column column) {}

    public DepositLayout(BlockPos center, double rotation, long seed) {
        this.center = center.immutable(); this.rotation = rotation; this.seed = seed;
        cosine = Math.cos(Math.toRadians(rotation)); sine = Math.sin(Math.toRadians(rotation));
    }
    public BlockPos center() { return center; }
    public long seed() { return seed; }
    public double localU(double x, double z) { return (x - center.getX()) * cosine + (z - center.getZ()) * sine; }
    public double localV(double x, double z) { return -(x - center.getX()) * sine + (z - center.getZ()) * cosine; }
    public BlockPos world(double u, double w, double v) {
        return BlockPos.containing(center.getX() + u * cosine - v * sine, center.getY() + w,
                center.getZ() + u * sine + v * cosine);
    }
    public boolean intersects(int minX, int minZ, int maxX, int maxZ) {
        return minX <= center.getX() + 124 && maxX >= center.getX() - 124
                && minZ <= center.getZ() + 124 && maxZ >= center.getZ() - 124;
    }
    /** Cheap rotated-box rejection before constructing per-column cave/pool fields. */
    public boolean intersectsLocal(ChunkPos chunk, double minU, double maxU, double minV, double maxV) {
        double x = chunk.getMinBlockX() + 7.5, z = chunk.getMinBlockZ() + 7.5;
        double u = localU(x, z), v = localV(x, z);
        double radius = 7.5 * (Math.abs(cosine) + Math.abs(sine));
        return u + radius >= minU && u - radius <= maxU && v + radius >= minV && v - radius <= maxV;
    }
    public boolean containsBiome(int x, int y, int z) {
        double u = localU(x, z) / 104, v = localV(x, z) / 60, w = y - center.getY();
        return w >= -22 && w <= 44 && square(square(u)) + square(square(v)) <= 1;
    }
    public Column column(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        int slot = (int) mix(key) & 255;
        Entry existing = columns.get(slot);
        if (existing != null && existing.key == key) return existing.column;
        Column result = buildColumn(x, z);
        columns.set(slot, new Entry(key, result));
        return result;
    }
    private Column buildColumn(int x, int z) {
        double u = localU(x, z), v = localV(x, z);
        double rough = field(x, z, seed + 21);
        double warpedU = u + field(x, z, seed + 22) * 1.2;
        double warpedV = v + field(x, z, seed + 23) * 1.0;
        Column out = new Column(u, v, center.getY(), rough);
        // Reuse the column fields for broad, walkable floor relief and leaning walls.
        double floor = 2.2 * rough + 1.1 * (warpedU - u) + .9 * (warpedV - v) + .012 * u - .015 * v;
        for (int i = 0; i < ROOMS.length; i++) {
            double[] room = ROOMS[i];
            double a = (warpedU - room[0]) / room[2], b = (warpedV - room[1]) / room[3];
            double norm = i == 0 ? Math.sqrt(Math.sqrt(square(square(a)) + square(square(b)))) : Math.sqrt(a * a + b * b);
            double horizontal = (norm - 1) * Math.min(room[2], room[3]);
            double bottom = room[4] + floor;
            double roof;
            if (i == 0) {
                double joint = v + .25 * u - 6 + .8 * rough;
                double blend = smooth((joint + 4) / 8);
                double planeA = 19 + .035 * u + .025 * v + .45 * rough;
                double planeB = 22 - .025 * u + .020 * v + .45 * rough;
                roof = Mth.lerp(blend, planeA, planeB) - .8 * smooth((17 - Math.abs(u - 7)) / 6) * ridge(joint, 1.7);
                roof = Mth.clamp(roof, 13, 26);
            } else roof = bottom + room[5] * (.7 + .3 * Math.sqrt(Math.max(0, 1 - norm * norm)));
            out.add(horizontal, bottom, roof);
        }
        for (int i = 0; i + 1 < SPINE.length; i++) {
            double[] a = SPINE[i], b = SPINE[i + 1];
            double distance = segmentDistance(warpedU, warpedV, a[0], a[1], b[0], b[1]);
            out.add(distance - (i == 0 || i == SPINE.length - 2 ? 3.6 : 4.0), floor + .3, floor + 9.5);
        }
        out.add(segmentDistance(warpedU, warpedV, -12, -10, -12, -26) - 3.1, floor + .5, floor + 9);
        applyPools(out, warpedU, warpedV, rough);
        out.finish();
        return out;
    }

    private static void applyPools(Column out, double u, double v, double rough) {
        double basin = Double.POSITIVE_INFINITY, rim = Double.NEGATIVE_INFINITY;
        for (Pool pool : POOLS) {
            double q = square((u - pool.u) / pool.radiusU) + square((v - pool.v) / pool.radiusV);
            if (q < out.poolQ) { out.poolQ = q; out.poolLevel = pool.level; }
            if (q < 1) basin = Math.min(basin, pool.level - .4 - pool.depth * (1 - q));
            else if (q < 1.8) {
                double shelf = pool.level + (q < 1.3 ? .4 : 1.2);
                rim = Math.max(rim, shelf * smooth((1.8 - q) / .3));
            }
        }
        // A connected rock tongue breaks up the main water surface, keeping the bank irregular.
        if (basin < Double.POSITIVE_INFINITY) {
            double tongue = smooth(1 - segmentDistance(u, v, -21, -9, -13, 0) / 3.5);
            basin = Mth.lerp(tongue, basin, .5);
        }
        for (int i = 0; i < out.count; i++) {
            int at = i * 3;
            if (out.parts[at] > 0) continue;
            if (basin < Double.POSITIVE_INFINITY) out.parts[at + 1] = Math.min(out.parts[at + 1], basin);
            else if (rim > Double.NEGATIVE_INFINITY) out.parts[at + 1] = Math.max(out.parts[at + 1], rim);
        }
        out.sedimentStrength = smooth((1.85 - out.poolQ) / .4) * smooth((out.poolQ - .4) / .4)
                * Mth.clamp(.5 + rough + (v > 2 ? .35 : 0), 0, 1);
    }

    public static final class Column {
        public final double u, v;
        public final int bankY;
        private final double lean;
        private final double[] parts = new double[36];
        private int count;
        public double horizontal, floor, roof, poolQ = Double.POSITIVE_INFINITY, poolLevel, sedimentStrength;
        private Column(double u, double v, int bankY, double lean) {
            this.u = u; this.v = v; this.bankY = bankY; this.lean = lean;
        }
        private void add(double horizontal, double floor, double roof) {
            int offset = count++ * 3; parts[offset] = horizontal; parts[offset + 1] = floor; parts[offset + 2] = roof;
        }
        private void finish() {
            int nearest = 0;
            for (int i = 1; i < count; i++) if (parts[i * 3] < parts[nearest * 3]) nearest = i;
            horizontal = parts[nearest * 3]; floor = parts[nearest * 3 + 1]; roof = parts[nearest * 3 + 2];
        }
        public double density(double worldY) {
            double w = worldY - bankY, value = boundaryDistance(worldY) / 3;
            // A low connected rib interrupts sightlines without closing the main hall.
            double q = square((u - 16) / 6) + square((v + 5) / 3.5);
            double rib = Math.min((1 - q) * 2, Math.min((w + .6) / 2, (5.5 - w) / 2));
            return Mth.clamp(Math.max(value, rib), -4, .8);
        }
        public double boundaryDistance(double worldY) {
            double w = worldY - bankY, value = Double.POSITIVE_INFINITY;
            for (int i = 0; i < count; i++) {
                int at = i * 3;
                double height = parts[at + 2] - parts[at + 1];
                double t = Mth.clamp((w - parts[at + 1]) / height, 0, 1);
                double horizontal = parts[at] - 2.2 * (4 * t * (1 - t)) + .15 * (w - parts[at + 1] - height / 2) * lean;
                double vertical = Math.max(parts[at + 1] - w, w - parts[at + 2]);
                double radius = Math.min(4.5, height * .3);
                double blend = Math.max(radius - Math.abs(horizontal - vertical), 0);
                double distance = Math.max(horizontal, vertical) + blend * blend / (4 * radius);
                value = Math.min(value, distance);
            }
            return Math.max(value, ridgeDistance(worldY));
        }
        public double terrainWeight(double worldY) {
            double w = worldY - bankY;
            return smooth(Math.min(Math.min((96 - Math.abs(u)) / 15, (48 - Math.abs(v)) / 12),
                    Math.min((w + 16) / 8, (38 - w) / 8)));
        }
        public double aquiferWeight(double worldY) {
            double w = worldY - bankY;
            return smooth(Math.min(Math.min((116 - Math.abs(u)) / 16, (68 - Math.abs(v)) / 16),
                    Math.min((w + 36) / 16, (60 - w) / 16)));
        }
        public boolean hasRidgeColumn() {
            return u > -24 && u < 2 && v > -5 && v < 30;
        }
        /** Positive inside a solid rib. All three lobes extend below the existing pool floor. */
        public double ridgeDistance(double worldY) {
            double w = worldY - bankY;
            if (!hasRidgeColumn() || w < -6 || w > 17) return -100;
            double body = -100;
            for (RidgeRock rock : RIDGE_ROCKS) {
                double du = u - rock.u - rock.tiltU * (w + 6);
                double dv = v - rock.v - rock.tiltV * (w + 6);
                double sides = Math.min(Math.min(rock.halfU - Math.abs(du), rock.halfV - Math.abs(dv)),
                        .8 * (rock.halfU + rock.halfV) - Math.abs(du) - Math.abs(dv));
                double top = rock.top + .18 * du - .10 * dv + .6 * lean - w;
                body = Math.max(body, Math.min(sides, Math.min(top, w + 6)));
            }
            return body;
        }
        /** Three continuous mineral-filled joints cut across the actual solid volume. */
        public double ridgeSeamDistance(double worldY) {
            double w = worldY - bankY;
            double frontJoint = Math.abs(v - 6.5 + .22 * (u + 14) - .08 * w) / 1.027;
            double backJoint = Math.abs(v - 14.5 - .16 * (u + 10) + .10 * w) / 1.018;
            double lengthwiseJoint = Math.abs(u + .25 * v + 10 - .14 * w) / 1.041;
            return Math.min(frontJoint, Math.min(backJoint, lengthwiseJoint));
        }
    }

    public static long mix(long value) {
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }
    public static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }
    public long hash(int x, int y, int z, long salt) { return mix(seed ^ salt ^ x * 0x632be59bd9b4e019L ^ y * 0x85157af5L ^ z * 0x9e3779b97f4a7c15L); }
    private static double square(double value) { return value * value; }
    private static double smooth(double value) { double t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t); }
    private static double ridge(double distance, double width) { return smooth(1 - Math.abs(distance) / width); }
    private static double segmentDistance(double x, double z, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double t = Mth.clamp(((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz), 0, 1);
        return Math.hypot(x - ax - t * dx, z - az - t * dz);
    }
    private static double field(int x, int z, long salt) {
        int gx = Math.floorDiv(x, 17), gz = Math.floorDiv(z, 17);
        double tx = smooth(Math.floorMod(x, 17) / 17.0), tz = smooth(Math.floorMod(z, 17) / 17.0);
        return Mth.lerp(tz, Mth.lerp(tx, lattice(gx, gz, salt), lattice(gx + 1, gz, salt)),
                Mth.lerp(tx, lattice(gx, gz + 1, salt), lattice(gx + 1, gz + 1, salt)));
    }
    private static double lattice(int x, int z, long salt) { return unit(mix(salt ^ x * 0x632be59bd9b4e019L ^ z * 0x9e3779b97f4a7c15L)) * 2 - 1; }
}
