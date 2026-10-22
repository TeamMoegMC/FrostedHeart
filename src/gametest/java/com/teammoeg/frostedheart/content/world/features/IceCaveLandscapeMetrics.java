package com.teammoeg.frostedheart.content.world.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Opt-in measurement only. A face is one square block face, not an ice volume proxy. */
public final class IceCaveLandscapeMetrics {
    public static final List<String> ORES = List.of("pyrite", "pyrolusite", "zinc", "lead", "silver");
    public static final int MIN_Y = Integer.getInteger("frostedheart.iceCaveMetricMinY", -63);
    public static final int MAX_Y = Integer.getInteger("frostedheart.iceCaveMetricMaxY", 129);
    private record Face(BlockPos pos, Direction normal, String category) {}
    private record PitColumn(int depth, int air, int water, boolean floorObserved,
                             boolean sampleClipped, boolean beyondDepthLimit, boolean allWaterSources,
                             boolean floorOutsideInclude) {}
    private final List<Face> before = new ArrayList<>();
    private final Set<BlockPos> waterTops = new HashSet<>();
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private final Map<String, Set<Face>> exposedOre = new LinkedHashMap<>();
    private final Map<String, Long> oreTotals = new LinkedHashMap<>();
    private final List<Map<String, Object>> views = new ArrayList<>();
    private final Map<String, Integer> viewScores = new HashMap<>();
    private final Map<Long, Integer> undergroundTops = new HashMap<>();
    private final Map<Long, Predicate<BlockPos>> sampledChunks = new HashMap<>();
    private final Map<String, Long> excludedLegacySolidBlocks = new LinkedHashMap<>();
    private final List<BlockPos> crystalViewCandidates = new ArrayList<>();

    public IceCaveLandscapeMetrics() {
        for (String ore : ORES) { oreTotals.put(ore, 0L); exposedOre.put(ore, new HashSet<>()); }
        for (String key : List.of("ice_crystal", "large_ice_crystal", "large_ice_crystal_exposed_to_air", "glowstone_crystal", "ice_crystal_exposed_to_air",
                "glowstone_crystal_exposed_to_air")) counters.put(key, 0L);
    }

    /** Called while the interior and its border are CARVERS, before FEATURES. */
    public void beforeChunk(ServerLevel level, int chunkX, int chunkZ, Predicate<BlockPos> include) {
        Map<Long, ChunkAccess> chunks = new HashMap<>();
        ChunkAccess center = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.CARVERS, true);
        if (center.getStatus().isOrAfter(ChunkStatus.FEATURES))
            throw new IllegalStateException("Landscape baseline must precede FEATURES: " + center.getPos() + " status=" + center.getStatus());
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
            undergroundTops.put(BlockPos.asLong(chunkX * 16 + x, 0, chunkZ * 16 + z),
                    center.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1 - 6);
        Function<BlockPos, BlockState> state = pos -> chunks.computeIfAbsent(
                net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4),
                key -> level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.CARVERS, true))
                .getBlockState(pos);
        scan(chunkX, chunkZ, pos -> {
            if (!include.test(pos) || !underground(pos)) return;
            BlockState block = state.apply(pos);
            if (block.is(Blocks.WATER) && block.getFluidState().isSource() && state.apply(pos.above()).isAir())
                waterTops.add(pos.immutable());
            if (!surface(block, center, pos)) return;
            for (Direction direction : Direction.values()) {
                BlockPos air = pos.relative(direction);
                if (state.apply(air).isAir()) {
                    before.add(new Face(pos.immutable(), direction, category(direction)));
                    if (direction.getAxis().isHorizontal() && state.apply(air.below()).isAir()
                            && state.apply(air.below(2)).isAir() && state.apply(air.below(3)).isAir())
                        before.add(new Face(pos.immutable(), direction, "high_wall"));
                }
            }
        });
    }

    /** Counts all ores in the same height/volume, including global veins and geodes. */
    public void afterChunk(ServerLevel level, int chunkX, int chunkZ, Predicate<BlockPos> include) {
        sampledChunks.put(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ), include);
        scan(chunkX, chunkZ, pos -> {
            if (!include.test(pos)) return;
            add("sampled_blocks", 1);
            BlockState block = level.getBlockState(pos);
            String id = BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            if (id.equals("frostedheart:ice_crystal") || id.equals("frostedheart:large_ice_crystal") || id.equals("frostedheart:glowstone_crystal")) {
                if (id.equals("frostedheart:ice_crystal") || id.equals("frostedheart:large_ice_crystal")) crystalViewCandidates.add(pos.immutable());
                String crystal = id.substring(id.indexOf(':') + 1);
                add(crystal, 1);
                for (Direction side : Direction.values()) {
                    if (level.getBlockState(pos.relative(side)).isAir()) {
                        add(crystal + "_exposed_to_air", 1);
                        break;
                    }
                }
            }
            String ore = ore(block);
            if (ore != null) oreTotals.merge(ore, 1L, Long::sum);
            if (!underground(pos)) return;
            if (block.isAir()) { maybeView(level, pos); return; }
            if (!surface(block, level, pos)) {
                if (block.isSolid() && block.getFluidState().isEmpty()) {
                    excludedLegacySolidBlocks.merge(id, 1L, Long::sum);
                    for (Direction direction : Direction.values()) {
                        BlockPos air = pos.relative(direction);
                        if (!level.getBlockState(air).isAir()) continue;
                        add("excluded_legacy_partial_" + category(direction) + "_faces", 1);
                        if (direction.getAxis().isHorizontal() && highWall(level, air))
                            add("excluded_legacy_partial_high_wall_faces", 1);
                    }
                }
                return;
            }
            for (Direction direction : Direction.values()) {
                BlockPos air = pos.relative(direction);
                if (!openSurfaceNeighbor(level, air)) continue;
                if (!level.getBlockState(air).isAir()) add("final_faces_adjacent_to_partial_decoration", 1);
                String kind = category(direction);
                countFace(kind, block);
                if (direction.getAxis().isHorizontal() && highWall(level, air)) countFace("high_wall", block);
                if (ore != null) {
                    add("exposed_ore_faces", 1);
                    exposedOre.get(ore).add(new Face(pos.immutable(), direction, kind));
                }
            }
        });
    }

    public Map<String, Object> finish(ServerLevel level) {
        for (Face face : before) {
            add("before_" + face.category + "_faces", 1);
            if (ice(level.getBlockState(face.pos)) || ice(level.getBlockState(face.pos.relative(face.normal))))
                add("before_" + face.category + "_covered_after", 1);
        }
        for (BlockPos pos : waterTops) {
            BlockState block = level.getBlockState(pos);
            if (thin(block)) add("lake_thin_ice", 1);
            else if (block.is(Blocks.ICE) || block.is(Blocks.PACKED_ICE)) add("lake_stable_ice", 1);
            else if (ice(block)) add("lake_other_ice", 1);
            else if (block.is(Blocks.WATER) && ice(level.getBlockState(pos.above()))) {
                BlockState cap = level.getBlockState(pos.above());
                add(thin(cap) ? "lake_thin_ice" : cap.is(Blocks.ICE) || cap.is(Blocks.PACKED_ICE)
                        ? "lake_stable_ice" : "lake_other_ice", 1);
            } else add("lake_uncovered_or_changed", 1);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("min_y_inclusive", MIN_Y);
        report.put("max_y_exclusive", MAX_Y);
        report.put("surface_definition", "Final exposed full-collision-cube block faces, plus stable/thin ice; ores remain in denominator. "
                + "Air or fluid-free partial non-ice decorations beside a face count as open space. High wall requires three such open blocks below its adjacent space. "
                + "This is a complete block-face area approximation, not pixel-exact occlusion by icicles/crystal clusters. These partial decorations are not surface blocks. "
                + "Full-cube glowstone crystal remains a non-ice solid. isSolid/legacySolid is not a full-cube test. "
                + "Before surfaces retain strict air-only adjacency at CARVERS, covered if original block or face-adjacent block is ice after FEATURES. "
                + "Surface and lake columns stop at the original WORLD_SURFACE_WG height minus 6; ore volume retains the full reported Y range.");
        report.put("primary_coverage_metric", "The original_area_covered_fraction fields; final_ice_fraction fields describe the final block-face approximation separately.");
        report.put("lake_definition", "Ordinary source-water top faces at CARVERS, before added traps; both same-cell freeze and cap-above count. "
                + "Uncovered/changed positions are retained in denominator. Springs are not ordinary water.");
        report.put("counts", counters);
        report.put("legacy_solid_partial_blocks_excluded", excludedLegacySolidBlocks);
        Map<String, Object> ratios = new LinkedHashMap<>();
        for (String kind : List.of("floor", "wall", "high_wall", "ceiling")) {
            ratios.put(kind + "_final_ice_fraction", ratio("final_" + kind + "_ice", "final_" + kind + "_faces"));
            ratios.put(kind + "_original_area_covered_fraction",
                    ratio("before_" + kind + "_covered_after", "before_" + kind + "_faces"));
        }
        report.put("surface_ratios", ratios);
        long thin = counters.getOrDefault("lake_thin_ice", 0L), stable = counters.getOrDefault("lake_stable_ice", 0L);
        Map<String, Object> lakes = new LinkedHashMap<>();
        lakes.put("original_water_top_faces", waterTops.size());
        lakes.put("covered_fraction", waterTops.isEmpty() ? null : (thin + stable + counters.getOrDefault("lake_other_ice", 0L)) / (double) waterTops.size());
        lakes.put("thin_fraction_of_original_top", waterTops.isEmpty() ? null : thin / (double) waterTops.size());
        lakes.put("stable_fraction_of_original_top", waterTops.isEmpty() ? null : stable / (double) waterTops.size());
        report.put("lakes", lakes);
        report.put("crystals", Map.of("ice_crystal", counters.get("ice_crystal"),
                "large_ice_crystal", counters.get("large_ice_crystal"),
                "large_ice_crystal_exposed_to_air", counters.get("large_ice_crystal_exposed_to_air"),
                "glowstone_crystal", counters.get("glowstone_crystal"),
                "ice_crystal_exposed_to_air", counters.get("ice_crystal_exposed_to_air"),
                "glowstone_crystal_exposed_to_air", counters.get("glowstone_crystal_exposed_to_air")));
        report.put("crystal_definition", "All final crystal blocks in the same included sample Y/volume as ore totals. "
                + "Exposed means at least one of six adjacent blocks is air; each crystal counts once. "
                + "This confirms open-air inlays, not camera line of sight or visibility through transparent ice.");
        Map<String, Object> trapReport = traps(level);
        report.put("traps", trapReport);
        Map<String, Object> ores = new LinkedHashMap<>();
        for (String ore : ORES) ores.put(ore, Map.of("all_final_blocks", oreTotals.get(ore),
                "exposed_faces", exposedOre.get(ore).size(), "largest_coplanar_exposed_face_patch_clipped_to_sample", largestPatch(exposedOre.get(ore))));
        report.put("ores", ores);
        addFeatureViews(level, trapReport);
        report.put("suggested_views", views);
        Map<String, Boolean> available = new LinkedHashMap<>();
        for (String kind : List.of("hall", "passage", "medium", "lake", "ice_crystal", "trap"))
            available.put(kind, views.stream().anyMatch(view -> view.get("kind").equals(kind)));
        report.put("suggested_view_availability", available);
        report.put("spring_status", "Natural generation and survival warming remain unaccepted; telemetry is not a pass requirement.");
        return report;
    }

    public List<Map<String, Object>> views() { return views; }

    private Map<String, Object> traps(ServerLevel level) {
        Set<BlockPos> lids = new HashSet<>();
        for (Face face : before) {
            if (face.normal != Direction.UP || !face.category.equals("floor")) continue;
            for (BlockPos candidate : List.of(face.pos, face.pos.above())) {
                if (sampled(candidate) && !waterTops.contains(candidate)
                        && thin(level.getBlockState(candidate))) lids.add(candidate);
            }
        }
        Set<BlockPos> remaining = new HashSet<>(lids);
        List<Map<String, Object>> groups = new ArrayList<>();
        int complete = 0, clipped = 0, dry = 0, wet = 0, mixed = 0, valid = 0;
        while (!remaining.isEmpty()) {
            // Stable anchors make repeated JSON reports comparable even though the membership set is hashed.
            BlockPos anchor = remaining.stream().min(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY)
                    .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).orElseThrow();
            List<BlockPos> group = new ArrayList<>();
            remaining.remove(anchor);
            group.add(anchor);
            boolean horizontalClipped = false, verticalClipped = false, missingBaseline = false;
            boolean floorsObserved = true, overflow = false, sourceWater = true;
            int minDepth = Integer.MAX_VALUE, maxDepth = 0, air = 0, water = 0, floorsOutsideInclude = 0;
            for (int i = 0; i < group.size(); i++) {
                BlockPos lid = group.get(i);
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = lid.relative(side);
                    if (!sampled(neighbor)) horizontalClipped = true;
                    else if (remaining.remove(neighbor)) group.add(neighbor);
                    else if (!lids.contains(neighbor) && !waterTops.contains(neighbor)
                            && thin(level.getBlockState(neighbor))) missingBaseline = true;
                }
                PitColumn column = pitColumn(level, lid);
                minDepth = Math.min(minDepth, column.depth);
                maxDepth = Math.max(maxDepth, column.depth);
                air += column.air;
                water += column.water;
                verticalClipped |= column.sampleClipped;
                floorsObserved &= column.floorObserved;
                overflow |= column.beyondDepthLimit;
                sourceWater &= column.allWaterSources;
                if (column.floorOutsideInclude) floorsOutsideInclude++;
            }
            boolean sampleClipped = horizontalClipped || verticalClipped;
            boolean fullyObserved = !sampleClipped && !missingBaseline && floorsObserved && !overflow;
            boolean depthValid = minDepth >= 3 && maxDepth <= 12;
            boolean consistent = (air > 0 || water > 0) && !(air > 0 && water > 0);
            String fill = air > 0 && water > 0 ? "mixed" : water > 0 ? "wet" : air > 0 ? "dry" : "no_observed_cavity";
            if (sampleClipped) clipped++;
            if (air > 0 && water > 0) mixed++;
            if (fullyObserved) {
                complete++;
                if (depthValid) {
                    valid++;
                    if (consistent && water > 0 && sourceWater) wet++;
                    else if (consistent && air > 0) dry++;
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lid_anchor_xyz", List.of(anchor.getX(), anchor.getY(), anchor.getZ()));
            item.put("lid_blocks", group.size());
            item.put("sample_clipped_horizontal", horizontalClipped);
            item.put("sample_clipped_vertical", verticalClipped);
            item.put("adjacent_thin_lid_without_original_solid_floor_baseline", missingBaseline);
            item.put("fully_observed", fullyObserved);
            item.put("min_observed_depth", minDepth);
            item.put("max_observed_depth", maxDepth);
            item.put("depth_within_3_to_12", depthValid);
            item.put("all_column_floors_observed", floorsObserved);
            item.put("floor_columns_outside_included_biome", floorsOutsideInclude);
            item.put("extends_to_thirteenth_cavity_cell", overflow);
            item.put("fill", fill);
            item.put("fill_consistent", consistent);
            item.put("observed_air_cells", air);
            item.put("observed_water_cells", water);
            item.put("all_observed_water_is_source", water == 0 ? null : sourceWater);
            groups.add(item);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("definition", "Final thin-ice lids at original CARVERS solid floor faces or one block above them (ice shells over ore); "
                + "ordinary natural water tops are excluded. Lids group by horizontal face adjacency at identical Y. "
                + "Depth counts consecutive air or ordinary water cells immediately below each lid, observing at most 13 "
                + "to distinguish the allowed 3..12 range. Fully observed groups require in-sample boundaries, observed solid "
                + "floors and no adjacent unknown-baseline lids. Unmodified solid retaining floors may belong to another "
                + "3D biome and are observed within the sampled chunk/Y bounds even when excluded from biome counts; "
                + "this is recorded separately rather than called a vertical sample cut. Cavity cells still require inclusion. "
                + "This cannot identify lids raised more than one block above the original solid floor. "
                + "Wet frequency uses only fully observed, valid-depth, consistently dry or source-water-filled groups; "
                + "an empty denominator is unavailable, not a pass.");
        report.put("candidate_lid_blocks_on_original_solid_floor", lids.size());
        report.put("connected_lid_groups", groups.size());
        report.put("fully_observed_groups", complete);
        report.put("sample_clipped_groups", clipped);
        report.put("fully_observed_valid_depth_groups", valid);
        report.put("fully_observed_consistent_dry_groups", dry);
        report.put("fully_observed_consistent_source_water_groups", wet);
        report.put("mixed_fill_groups_including_clipped", mixed);
        report.put("wet_fraction_of_complete_consistent_valid_groups", dry + wet == 0 ? null : wet / (double)(dry + wet));
        report.put("groups", groups);
        return report;
    }

    private PitColumn pitColumn(ServerLevel level, BlockPos lid) {
        int depth = 0, air = 0, water = 0;
        boolean sources = true;
        for (int down = 1; down <= 13; down++) {
            BlockPos pos = lid.below(down);
            // The producer requires the carved cavity, not its unmodified supporting floor, to be in ice_caves.
            // A solid floor across a 3D biome boundary remains observable inside the physical sample volume.
            if (!withinTrapSampleBounds(pos)) return new PitColumn(depth, air, water, false, true, false, sources, false);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.WATER))
                return new PitColumn(depth, air, water, surface(state, level, pos), false, false, sources, !sampled(pos));
            if (!sampled(pos)) return new PitColumn(depth, air, water, false, true, false, sources, false);
            depth++;
            if (state.isAir()) air++;
            else { water++; sources &= state.getFluidState().isSource(); }
        }
        return new PitColumn(depth, air, water, false, false, true, sources, false);
    }

    private boolean withinTrapSampleBounds(BlockPos pos) {
        return pos.getY() >= MIN_Y && pos.getY() < MAX_Y && sampledChunks.containsKey(
                net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    private boolean sampled(BlockPos pos) {
        if (pos.getY() < MIN_Y || pos.getY() >= MAX_Y) return false;
        Predicate<BlockPos> include = sampledChunks.get(
                net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return include != null && include.test(pos);
    }

    private void maybeView(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.above()).isAir() || !surface(level.getBlockState(pos.below()), level, pos.below())) return;
        int height = 2;
        while (height < 24 && level.getBlockState(pos.above(height)).isAir()) height++;
        String kind = height >= 8 && IceCaveSpace.isWide(level, pos) ? "hall" : height <= 5 ? "passage" : "medium";
        int horizontal = 0, best = 0, second = 0;
        Direction heading = Direction.SOUTH, alternate = Direction.NORTH;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int distance = 0;
            for (int n = 1; n <= 16; n++) {
                BlockPos next = pos.relative(direction, n);
                if (!level.getBlockState(next.above()).isAir() || !level.getBlockState(next).isAir()) break;
                distance++;
            }
            horizontal += distance;
            if (distance > best) { second = best; alternate = heading; best = distance; heading = direction; }
            else if (distance > second) { second = distance; alternate = direction; }
        }
        // Prefer a broad floor and tall room, then point the camera along its longest clear ray.
        int score = horizontal * 4 + height * 2 + best;
        if (best < 4 || score <= viewScores.getOrDefault(kind, -1)) return;
        viewScores.put(kind, score);
        views.removeIf(view -> view.get("kind").equals(kind));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("kind", kind); view.put("x", pos.getX() + .5); view.put("y", pos.getY()); view.put("z", pos.getZ() + .5);
        view.put("clear_height", height); view.put("horizontal_cardinal_clearance_sum", horizontal);
        view.put("look_clearance", best); view.put("alternate_clearance", second);
        view.put("yaw", heading.toYRot()); view.put("alternate_yaw", alternate.toYRot()); view.put("pitch", -8);
        views.add(view);
    }

    private void addFeatureViews(ServerLevel level, Map<String, Object> trapReport) {
        // Prefer a water-top point surrounded by other sampled water tops, instead of the first shoreline cell.
        List<BlockPos> lakeCandidates = waterTops.stream().sorted(java.util.Comparator
                .<BlockPos>comparingInt(this::nearbyWaterTops).reversed().thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).toList();
        for (BlockPos original : lakeCandidates) {
            BlockPos cap = ice(level.getBlockState(original)) ? original : original.above();
            if (ice(level.getBlockState(cap)) && addObliqueView(level, "lake", cap)) break;
        }
        for (BlockPos crystal : crystalViewCandidates) {
            BlockState state = level.getBlockState(crystal);
            if (!state.hasProperty(BlockStateProperties.FACING)) continue;
            Direction facing = state.getValue(BlockStateProperties.FACING);
            boolean corridor = true;
            for (int n = 1; n <= 3; n++) if (!loadedAir(level, crystal.relative(facing, n))) { corridor = false; break; }
            if (!corridor) continue;
            Vec3 target = Vec3.atCenterOf(crystal);
            Vec3 eye = target.add(facing.getStepX() * 3, facing.getStepY() * 3 + .25, facing.getStepZ() * 3);
            if (addTargetView(level, "ice_crystal", eye, target, crystal)) break;
        }
        for (Object entry : (List<?>) trapReport.get("groups")) {
            Map<?, ?> group = (Map<?, ?>) entry;
            List<?> xyz = (List<?>) group.get("lid_anchor_xyz");
            BlockPos lid = new BlockPos(((Number) xyz.get(0)).intValue(), ((Number) xyz.get(1)).intValue(), ((Number) xyz.get(2)).intValue());
            if (thin(level.getBlockState(lid)) && addObliqueView(level, "trap", lid)) break;
        }
    }

    private int nearbyWaterTops(BlockPos pos) {
        int count = 0;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (waterTops.contains(pos.offset(x, 0, z))) count++;
        return count;
    }

    private boolean addObliqueView(ServerLevel level, String kind, BlockPos top) {
        Vec3 target = Vec3.atCenterOf(top).add(0, .5, 0);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 eye = target.add(direction.getStepX() * 3, 2.2, direction.getStepZ() * 3);
            if (addTargetView(level, kind, eye, target, top)) return true;
        }
        return false;
    }

    private boolean addTargetView(ServerLevel level, String kind, Vec3 eye, Vec3 target, BlockPos feature) {
        if (views.stream().anyMatch(view -> view.get("kind").equals(kind))) return true;
        // The client teleports to y+.05 and spectator eye height is 1.62; leave its whole body in air.
        double footY = eye.y - 1.67;
        for (int x = (int) Math.floor(eye.x - .3); x <= (int) Math.floor(eye.x + .3); x++)
            for (int z = (int) Math.floor(eye.z - .3); z <= (int) Math.floor(eye.z + .3); z++)
                for (int y = (int) Math.floor(footY); y <= (int) Math.floor(footY + 1.85); y++)
                    if (!loadedAir(level, new BlockPos(x, y, z))) return false;
        int steps = (int) Math.ceil(eye.distanceTo(target) * 4);
        for (int i = 0; i < steps; i++) {
            Vec3 point = eye.lerp(target, i / (double) steps);
            BlockPos pos = BlockPos.containing(point);
            if (!pos.equals(feature) && !loadedAir(level, pos)) return false;
        }
        Vec3 delta = target.subtract(eye);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("kind", kind); view.put("x", eye.x); view.put("y", footY); view.put("z", eye.z);
        view.put("yaw", (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        view.put("pitch", (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        view.put("target_xyz", List.of(target.x, target.y, target.z));
        view.put("feature_block_xyz", List.of(feature.getX(), feature.getY(), feature.getZ()));
        view.put("camera_body_in_air", true); view.put("sampled_line_to_target_clear", true);
        views.add(view);
        return true;
    }

    private static boolean loadedAir(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).isAir();
    }

    private boolean underground(BlockPos pos) {
        return pos.getY() <= undergroundTops.getOrDefault(BlockPos.asLong(pos.getX(), 0, pos.getZ()), MIN_Y - 1);
    }

    private static boolean highWall(ServerLevel level, BlockPos air) {
        for (int down = 1; down <= 3; down++) if (!openSurfaceNeighbor(level, air.below(down))) return false;
        return true;
    }
    private static boolean openSurfaceNeighbor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getFluidState().isEmpty() && !surface(state, level, pos);
    }
    private void countFace(String kind, BlockState block) {
        add("final_" + kind + "_faces", 1);
        if (ice(block)) add("final_" + kind + "_ice", 1);
        if (ore(block) != null) add("final_" + kind + "_ore_faces_in_denominator", 1);
    }
    private static void scan(int chunkX, int chunkZ, java.util.function.Consumer<BlockPos> visit) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++)
            for (int y = MIN_Y; y < MAX_Y; y++) visit.accept(pos.set(x, y, z));
    }
    private static String category(Direction direction) { return direction == Direction.UP ? "floor" : direction == Direction.DOWN ? "ceiling" : "wall"; }
    private void add(String key, long value) { counters.merge(key, value, Long::sum); }
    private Double ratio(String numerator, String denominator) {
        long total = counters.getOrDefault(denominator, 0L);
        return total == 0 ? null : counters.getOrDefault(numerator, 0L) / (double) total;
    }
    private static boolean surface(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.isAir() && state.getFluidState().isEmpty() && (ice(state) || state.isCollisionShapeFullBlock(level, pos));
    }
    public static boolean ice(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return state.is(Blocks.ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.PACKED_ICE)
                || id.equals("frostedheart:firm_ice") || thin(state);
    }
    private static boolean thin(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return id.equals("frostedheart:thin_ice") || id.equals("frostedheart:layered_thin_ice");
    }
    private static String ore(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (id.endsWith("pyrite_ore")) return "pyrite";
        if (id.endsWith("pyrolusite_ore")) return "pyrolusite";
        if (id.endsWith("zinc_ore")) return "zinc";
        if (id.endsWith("ore_lead")) return "lead";
        if (id.endsWith("ore_silver")) return "silver";
        return null;
    }
    private static int largestPatch(Set<Face> faces) {
        Set<Face> remaining = new HashSet<>(faces);
        int max = 0;
        while (!remaining.isEmpty()) {
            List<Face> queue = new ArrayList<>();
            Face first = remaining.iterator().next(); remaining.remove(first); queue.add(first);
            for (int i = 0; i < queue.size(); i++) {
                Face face = queue.get(i);
                for (Direction direction : Direction.values()) if (direction.getAxis() != face.normal.getAxis()) {
                    Face next = new Face(face.pos.relative(direction), face.normal, face.category);
                    if (remaining.remove(next)) queue.add(next);
                }
            }
            max = Math.max(max, queue.size());
        }
        return max;
    }
}
