package com.teammoeg.frostedheart.content.world.features.deposit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in full-pack visual inspection. Packaged only in the separate validation JAR. */
@Mod.EventBusSubscriber(modid = "frostedheart", value = Dist.CLIENT)
public final class DepositClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedheart.depositClientValidation");
    private static final long SEED = Long.getLong("frostedheart.depositClientSeed", -4191132991717996371L);
    private static final Path OUTPUT = Path.of(System.getProperty("frostedheart.depositClientOutput", "deposit-visual-evidence"));
    private static final ResourceLocation THERMAL = new ResourceLocation(System.getProperty(
            "frostedheart.depositClientBiome", "the_winter_rescue:hydrothermal_deposits"));
    private static final long START = System.nanoTime();
    private static final List<Site> SITES = new ArrayList<>();
    private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
    private static volatile boolean pending, finished;
    private static volatile int stage;
    private static int siteIndex, chunkIndex, phase, cameraIndex, frames;
    private static long cameraReady, generationNanos;
    private static final List<Double> GENERATION_REQUEST_MS = new ArrayList<>();
    private record WaterSnapshot(int[] states, Map<String, Integer> counts, long nanos, int ticks) {}
    private static WaterSnapshot waterBefore;
    private static Site site;
    private static int startX, startZ, size;
    private static int[] blockCounts;
    private static int[] waterOtherCounts, airOtherCounts;
    private static long thermalQuarts, cavityQuarts, cavityThermalQuarts, expectedWater, actualWater, expectedAir, actualAir;
    private static long routeColumns, clearRouteColumns;
    private static final StringBuilder ROUTE = new StringBuilder();
    private static Map<String, Object> result;
    private static final Map<String, View> BEST = new LinkedHashMap<>();
    private static List<View> cameras;
    private static final Map<String, List<BlockPos>> INSPECTION_LIGHTS = new LinkedHashMap<>();
    private static final boolean DENSITY_PREVIEW = false;
    private static final boolean ART_PREVIEW = false;
    private static DensityFunction artCavity;
    private static final BlockPos PREVIEW_CENTER = new BlockPos(-1335, -10, -269);
    private static final double PREVIEW_COS = Math.cos(Math.toRadians(37));
    private static final double PREVIEW_SIN = Math.sin(Math.toRadians(37));

    private record Site(String name, BlockPos center, DepositLayout plan, int chunks) {}
    private record View(String kind, double x, double y, double z, float yaw, float pitch, double score) {}

    @SubscribeEvent
    public static void client(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.options.pauseOnLostFocus = false;
            if ((System.nanoTime() - START) / 1e9 > 1800) throw new IllegalStateException("Visual inspection timed out at stage " + stage);
            if (stage > 0 && mc.screen instanceof AlertScreen alert) throw new IllegalStateException(alert.getNarrationMessage().getString());
            if (stage == 0 && mc.getOverlay() == null && mc.screen instanceof AccessibilityOnboardingScreen onboarding) {
                onboarding.onClose();
            } else if (stage == 0 && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
                stage = 1;
                mc.options.renderDistance().set(Integer.getInteger("frostedheart.depositClientRenderDistance", 7));
                String existing = System.getProperty("frostedheart.depositClientWorld");
                if (existing != null) {
                    System.out.println("DEPOSIT_VISUAL reopening " + existing);
                    mc.createWorldOpenFlows().loadLevel(mc.screen, existing);
                    return;
                }
                mc.options.simulationDistance().set(5);
                String name = "deposit-inspection-" + System.currentTimeMillis();
                System.out.println("DEPOSIT_VISUAL creating " + name + " seed=" + SEED);
                mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings(name, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(SEED, true, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            } else if (stage == 1 && mc.player != null && mc.getSingleplayerServer() != null && !pending) {
                stage = 2;
                submit(DepositClientValidation::prepare);
            } else if (stage == 2 && !pending) {
                submit(DepositClientValidation::sampleStep);
            } else if (stage == 4 && mc.player != null) {
                View view = cameras.get(cameraIndex);
                if (DENSITY_PREVIEW && mc.screen != null) {
                    System.out.println("DEPOSIT_VISUAL closing capture overlay " + mc.screen.getClass().getName());
                    mc.setScreen(null);
                }
                mc.options.hideGui = true;
                mc.player.setYRot(view.yaw());
                mc.player.setXRot(view.pitch());
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void submit(Runnable work) {
        pending = true;
        Minecraft.getInstance().getSingleplayerServer().execute(() -> {
            try { work.run(); }
            catch (Throwable failure) { Minecraft.getInstance().execute(() -> finish(failure)); }
            finally { pending = false; }
        });
    }

    private static void prepare() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        ServerLevel level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(Integer.getInteger("frostedheart.depositClientRandomTicks", 0), server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.setWeatherParameters(0, 12000, false, false);
        level.setDayTime(6000);
        var player = server.getPlayerList().getPlayers().get(0);
        player.setGameMode(GameType.SPECTATOR);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 72000, 0, false, false));
        var regions = DepositRegions.resolve(level.getChunkSource().randomState().sampler().depth());
        if (regions == null) throw new IllegalStateException("Normal preset has no production region field");
        DepositLayout selected = null;
        for (int radius = 0; selected == null && radius <= 8; radius++)
            for (int x = -radius; x <= radius && selected == null; x++) for (int z = -radius; z <= radius && selected == null; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == radius)
                    selected = regions.layout(x * regions.cellSize() + regions.cellSize() / 2, z * regions.cellSize() + regions.cellSize() / 2);
            }
        if (selected == null) throw new IllegalStateException("No naturally selected deposit within search area");
        SITES.add(new Site("production-deposit", selected.center(), selected, 18));
        beginSite();
    }

    private static String describe(DepositLayout plan) {
        return plan == null ? "none" : plan.center().toShortString();
    }

    private static void beginSite() {
        site = SITES.get(siteIndex);
        size = site.chunks();
        startX = (site.center().getX() >> 4) - size / 2;
        startZ = (site.center().getZ() >> 4) - size / 2;
        chunkIndex = phase = 0;
        generationNanos = 0;
        GENERATION_REQUEST_MS.clear();
        blockCounts = new int[BuiltInRegistries.BLOCK.size()];
        waterOtherCounts = new int[blockCounts.length];
        airOtherCounts = new int[blockCounts.length];
        thermalQuarts = cavityQuarts = cavityThermalQuarts = expectedWater = actualWater = expectedAir = actualAir = 0;
        routeColumns = clearRouteColumns = 0;
        ROUTE.setLength(0);
        ROUTE.append("x,z,floor,roof,actual_clear\n");
        BEST.clear();
        result = new LinkedHashMap<>();
        result.put("name", site.name());
        result.put("seed", SEED);
        result.put("actual_level_seed", Minecraft.getInstance().getSingleplayerServer().overworld().getSeed());
        result.put("center", List.of(site.center().getX(), site.center().getY(), site.center().getZ()));
        result.put("planned_body_center", describe(site.plan()));
        result.put("sample_bounds", List.of(startX * 16, (startX + size) * 16 - 1, -63, 39, startZ * 16, (startZ + size) * 16 - 1));
        result.put("save_directory", Minecraft.getInstance().getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().toString());
        result.put("scope", "Production normal preset and world-seeded region selection. Full installed pack; fresh terrain, original ore/decorations. Spectator/night vision for capture, random ticks recorded; no layout injection.");
        if (DENSITY_PREVIEW) result.put("scope", "Full installed pack, test-only fixed density anchor and shared biome envelope, original hydrothermal background features, original aquifer. Legacy late body feature disabled in runtime data only. Fresh world; no terrain edits. Not random-placement or final-art acceptance.");
        if (ART_PREVIEW) result.put("scope", "Fixed art layout and shared biome envelope; local density replacement and reduced aquifer floodedness, finite ordinary pool water. Full installed pack with isolated staged data. Actual target biome/features and random tick speed are recorded below. Weather, mobs and daylight cycle disabled. Any inspection torches are recorded and are not generated content. Not random-placement or performance acceptance.");
        result.put("random_tick_speed", levelRandomTicks());
        System.out.println("DEPOSIT_VISUAL begin " + site.name() + " chunks=" + size * size);
    }

    private static void sampleStep() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().overworld();
        if (phase == 0) {
            int width = size + 2;
            long start = System.nanoTime();
            level.getChunk(startX - 1 + chunkIndex % width, startZ - 1 + chunkIndex / width);
            long elapsed = System.nanoTime() - start;
            generationNanos += elapsed;
            GENERATION_REQUEST_MS.add(elapsed / 1e6);
            if (++chunkIndex < width * width) return;
            phase = 1;
            chunkIndex = 0;
        }
        inspectChunk(level, startX + chunkIndex % size, startZ + chunkIndex / size);
        if (++chunkIndex < size * size) return;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < blockCounts.length; i++) if (blockCounts[i] > 0)
            counts.put(BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.byId(i)).toString(), blockCounts[i]);
        result.put("block_counts", counts);
        result.put("generation_request_seconds", generationNanos / 1e9);
        result.put("generation_request_ms", GENERATION_REQUEST_MS);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(THERMAL);
        if (biome != null) {
            result.put("target_biome", THERMAL.toString());
            result.put("actual_water_color", String.format("#%06X", biome.getWaterColor()));
            result.put("actual_base_temperature", biome.getBaseTemperature());
            result.put("actual_feature_steps", biome.getGenerationSettings().features().stream().map(step -> step.stream()
                    .map(holder -> holder.unwrapKey().map(key -> key.location().toString()).orElse("inline")).toList()).toList());
        }
        result.put("thermal_quart_cells", thermalQuarts);
        result.put("planned_cavity_quart_cells", cavityQuarts);
        result.put("planned_cavity_thermal_quart_cells", cavityThermalQuarts);
        result.put("expected_interior_water", expectedWater);
        result.put("actual_interior_water", actualWater);
        result.put("expected_interior_air", expectedAir);
        result.put("actual_interior_air", actualAir);
        result.put("geometric_water_cells_other_states", stateCounts(waterOtherCounts));
        result.put("geometric_air_cells_other_states", stateCounts(airOtherCounts));
        result.put("intended_route_columns", routeColumns);
        result.put("actual_clear_route_columns", clearRouteColumns);
        result.put("interior_measurement_scope", DENSITY_PREVIEW
                ? "All positions strictly inside the analytic padded core with zero roughness. Air counts are diagnostic: aquifer water and normal decoration are included as other states; this is not an all-air requirement or a leak assertion."
                : "Geometric interior excludes two bottom layers and one roof layer; remaining sediment skins and basalt details are included in other states, so a mismatch is not automatically a leak or failed placement.");
        if (ART_PREVIEW) result.put("interior_measurement_scope", "Positions where the registered sample/cavity field is negative. Interpolation, native aquifer, pools and decoration affect final states. The legacy expected_water and route fields are unused in this mode; saved-block audit measures actual water separately.");
        try {
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve(site.name() + "-route.csv"), ROUTE);
        } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        cameras = new ArrayList<>(BEST.values());
        String cameraFile = System.getProperty("frostedheart.depositClientCameras");
        if (cameraFile != null) {
            try {
                cameras.clear();
                for (var value : JsonParser.parseString(Files.readString(Path.of(cameraFile))).getAsJsonArray()) {
                    var view = value.getAsJsonObject();
                    double x = view.get("x").getAsDouble(), z = view.get("z").getAsDouble(), y = view.get("y").getAsDouble();
                    if (view.has("auto_floor") && view.get("auto_floor").getAsBoolean()) {
                        int preferred = (int) Math.floor(y), best = 1000;
                        for (int floor = preferred - 7; floor <= preferred + 6; floor++) {
                            BlockPos p = BlockPos.containing(x, floor, z);
                            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                                    && Math.abs(floor - preferred) < best) {
                                best = Math.abs(floor - preferred); y = floor;
                            }
                        }
                    }
                    float yaw = view.has("yaw") ? view.get("yaw").getAsFloat() : 0;
                    float pitch = view.has("pitch") ? view.get("pitch").getAsFloat() : 0;
                    if (view.has("target")) {
                        var target = view.getAsJsonArray("target");
                        double dx = target.get(0).getAsDouble() - x, dz = target.get(2).getAsDouble() - z;
                        yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
                        pitch = (float) -Math.toDegrees(Math.atan2(target.get(1).getAsDouble() - y - 1.62, Math.hypot(dx, dz)));
                    }
                    String kind = view.get("kind").getAsString();
                    var feet = BlockPos.containing(x, y, z);
                    @SuppressWarnings("unchecked")
                    var supportEvidence = (Map<String, Object>) result.computeIfAbsent("camera_supports", key -> new LinkedHashMap<>());
                    supportEvidence.put(kind, Map.of("feet", List.of(feet.getX(), feet.getY(), feet.getZ()),
                            "feet_air", level.getBlockState(feet).isAir(),
                            "head_air", level.getBlockState(feet.above()).isAir(),
                            "sturdy_floor", level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP),
                            "floor_state", level.getBlockState(feet.below()).toString()));
                    if (view.has("inspection_lights")) {
                        List<BlockPos> lights = new ArrayList<>();
                        for (var light : view.getAsJsonArray("inspection_lights")) {
                            var p = light.getAsJsonArray();
                            lights.add(BlockPos.containing(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()));
                        }
                        INSPECTION_LIGHTS.put(kind, lights);
                    }
                    cameras.add(new View(kind, x, y, z, yaw, pitch, 0));
                }
            } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        }

        if (cameras.isEmpty()) throw new IllegalStateException("No open camera position at " + site.name());
        cameraIndex = 0;
        System.out.println("DEPOSIT_VISUAL measured " + site.name() + " cameras=" + cameras.size()
                + " water=" + actualWater + "/" + expectedWater + " thermal=" + cavityThermalQuarts + "/" + cavityQuarts);
        try { writeResults(); } catch (Exception failure) { throw new RuntimeException(failure); }
        teleport();
    }

    private static void inspectChunk(ServerLevel level, int cx, int cz) {
        var chunk = level.getChunk(cx, cz);
        var pos = new BlockPos.MutableBlockPos();
        for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
            for (int y = -63; y < 40; y++) {
                pos.set(x, y, z);
                var state = chunk.getBlockState(pos);
                blockCounts[BuiltInRegistries.BLOCK.getId(state.getBlock())]++;
                if ((x & 3) == 0 && (z & 3) == 0 && (y & 3) == 0 && site.plan().containsBiome(x, y, z)) {
                    cavityQuarts++;
                    if (level.getBiome(pos).is(THERMAL)) cavityThermalQuarts++;
                }
            }
        }
        if (cx == (site.center().getX() >> 4) && cz == (site.center().getZ() >> 4)) {
            camerasForPlan(level);
        }
    }

    private static void camerasForPlan(ServerLevel level) {
        double[][] poses = {{-26, -14}, {8, -8}, {-38, -5}};
        String[] names = {"overview", "side", "entry"};
        for (int i = 0; i < poses.length; i++) {
            BlockPos desired = site.plan().world(poses[i][0], 0, poses[i][1]);
            BlockPos feet = null;
            search: for (int radius = 0; radius <= 6; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                for (int y = site.center().getY() + 8; y >= site.center().getY() - 8; y--) {
                    BlockPos p = new BlockPos(desired.getX() + dx, y, desired.getZ() + dz);
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir()
                            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) { feet = p; break search; }
                }
            }
            if (feet == null) continue;
            BlockPos target = site.plan().world(-10, 6, 10);
            double dx = target.getX() - feet.getX(), dz = target.getZ() - feet.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
            float pitch = (float) -Math.toDegrees(Math.atan2(target.getY() - feet.getY() - 1.62, Math.hypot(dx, dz)));
            BEST.put(names[i], new View(names[i], feet.getX() + .5, feet.getY(), feet.getZ() + .5, yaw, pitch, 0));
        }
    }

    private static void teleport() {
        Minecraft mc = Minecraft.getInstance();
        View view = cameras.get(cameraIndex);
        var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        if (INSPECTION_LIGHTS.containsKey(view.kind())) placeInspectionLights(mc.getSingleplayerServer().overworld(), view.kind());
        if (view.kind().startsWith("normal-")) player.removeEffect(MobEffects.NIGHT_VISION);
        else player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 72000, 0, false, false));
        player.teleportTo(mc.getSingleplayerServer().overworld(),
                view.x(), view.y(), view.z(), view.yaw(), view.pitch());
        if (view.kind().equals("water-observation") && Long.getLong("frostedheart.depositClientFluidSeconds", 0L) > 0)
            waterBefore = captureWater(mc.getSingleplayerServer().overworld());
        frames = 0;
        cameraReady = System.nanoTime();
        stage = 4;
    }

    private static Map<String, Integer> stateCounts(int[] counts) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                result.put(BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.byId(i)).toString(), counts[i]);
            }
        }
        return result;
    }

    private static int levelRandomTicks() {
        return Minecraft.getInstance().getSingleplayerServer().overworld().getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
    }

    private static WaterSnapshot captureWater(ServerLevel level) {
        int[] states = new int[109 * 91 * 16];
        Map<String, Integer> counts = new LinkedHashMap<>();
        var pos = new BlockPos.MutableBlockPos();
        int at = 0;
        for (int x = -1389; x <= -1281; x++) for (int z = -314; z <= -224; z++) for (int y = -42; y <= -27; y++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) || state.is(Blocks.BUBBLE_COLUMN) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                states[at] = Block.getId(state) + 1;
                counts.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1, Integer::sum);
            }
            at++;
        }
        return new WaterSnapshot(states, counts, System.nanoTime(), Minecraft.getInstance().getSingleplayerServer().getTickCount());
    }

    private static void finishWaterObservation() {
        var after = captureWater(Minecraft.getInstance().getSingleplayerServer().overworld());
        int changed = 0;
        List<Map<String, Object>> changes = new ArrayList<>();
        for (int i = 0; i < after.states.length; i++) if (after.states[i] != waterBefore.states[i]) {
            changed++;
            if (changes.size() < 256) {
                int x = -1389 + i / (91 * 16), z = -314 + i / 16 % 91, y = -42 + i % 16;
                changes.add(Map.of("position", List.of(x, y, z), "before", waterBefore.states[i] == 0 ? "other"
                        : Block.stateById(waterBefore.states[i] - 1).toString(), "after", after.states[i] == 0 ? "other"
                        : Block.stateById(after.states[i] - 1).toString()));
            }
        }
        result.put("fluid_changed_positions", changes);
        result.put("fluid_observation", Map.of("bounds", List.of(-1389, -1281, -42, -27, -314, -224),
                "wall_seconds", (after.nanos - waterBefore.nanos) / 1e9, "ticks", after.ticks - waterBefore.ticks,
                "before", waterBefore.counts, "after", after.counts, "water_or_ice_states_changed", changed,
                "random_tick_speed", levelRandomTicks(), "scope", "Existing rules and thermal runtime. This run adds no inspection torches during the interval; reopened saves may contain earlier lights. No replenishing water."));
        waterBefore = null;
    }

    /** Ordinary test torches on existing supports, only for cameras that explicitly request them. */
    private static void placeInspectionLights(ServerLevel level, String kind) {
        List<Map<String, Object>> placed = new ArrayList<>();
        for (BlockPos desired : INSPECTION_LIGHTS.remove(kind)) {
            boolean done = false;
            for (int radius = 0; radius <= 3 && !done; radius++) for (int dy : new int[]{0, -1, 1, -2, 2}) {
                if (done) break;
                for (int dx = -radius; dx <= radius && !done; dx++) for (int dz = -radius; dz <= radius && !done; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos pos = desired.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).isAir()) continue;
                    BlockState torch = null;
                    if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) torch = Blocks.TORCH.defaultBlockState();
                    else for (Direction side : Direction.Plane.HORIZONTAL) {
                        BlockPos support = pos.relative(side);
                        if (level.getBlockState(support).isFaceSturdy(level, support, side.getOpposite())) {
                            torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, side.getOpposite()); break;
                        }
                    }
                    if (torch == null) continue;
                    level.setBlock(pos, torch, 3);
                    placed.add(Map.of("position", List.of(pos.getX(), pos.getY(), pos.getZ()), "state", torch.toString()));
                    done = true;
                }
            }
        }
        result.put("inspection_lights", placed);
        System.out.println("DEPOSIT_VISUAL inspection torches=" + placed.size());
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || stage != 4 || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        View view = cameras.get(cameraIndex);
        if (mc.player.distanceToSqr(view.x(), view.y(), view.z()) > 4 || !mc.level.hasChunkAt(mc.player.blockPosition())) return;
        long captureSeconds = Long.getLong("frostedheart.depositClientCaptureSeconds", 10L);
        if (view.kind().equals("water-observation") && waterBefore != null) {
            captureSeconds = Long.getLong("frostedheart.depositClientFluidSeconds", 0L);
            if (mc.getSingleplayerServer().getTickCount() - waterBefore.ticks < captureSeconds * 20) return;
        }
        if (++frames < 100 || System.nanoTime() - cameraReady < captureSeconds * 1_000_000_000L) return;
        try {
            Files.createDirectories(OUTPUT);
            Path image = OUTPUT.resolve(site.name() + "-" + view.kind() + ".png");
            try (var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { screenshot.writeToFile(image); }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> images = (List<Map<String, Object>>) result.computeIfAbsent("screenshots", key -> new ArrayList<>());
            images.add(Map.of("kind", view.kind(), "x", view.x(), "y", view.y(), "z", view.z(), "yaw", view.yaw(), "pitch", view.pitch(), "image", image.toAbsolutePath().toString()));
            writeResults();
            System.out.println("DEPOSIT_VISUAL screenshot " + image);
            stage = 3;
            if (view.kind().equals("water-observation") && waterBefore != null)
                submit(() -> { finishWaterObservation(); mc.execute(DepositClientValidation::advanceCamera); });
            else advanceCamera();
        } catch (Throwable failure) { finish(failure); }
    }

    private static void advanceCamera() {
        if (++cameraIndex < cameras.size()) submit(DepositClientValidation::teleport);
        else {
            RESULTS.add(result);
            try { writeResults(); } catch (Exception failure) { finish(failure); return; }
            if (++siteIndex < SITES.size()) submit(() -> { beginSite(); stage = 2; });
            else finish(null);
        }
    }

    private static void writeResults() throws Exception {
        Files.createDirectories(OUTPUT);
        var snapshot = new ArrayList<>(RESULTS);
        if (result != null && !snapshot.contains(result)) snapshot.add(result);
        Files.writeString(OUTPUT.resolve("terrain-samples.json"), new GsonBuilder().setPrettyPrinting().create().toJson(snapshot));
    }

    private static void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        try {
            if (failure != null && result != null && !RESULTS.contains(result)) { result.put("failure", failure.toString()); RESULTS.add(result); }
            writeResults();
            Files.writeString(OUTPUT.resolve("result.txt"), failure == null ? "CAPTURE COMPLETE; inspect the actual PNGs.\n" : "FAILED: " + failure + "\n");
        } catch (Exception writeFailure) { writeFailure.printStackTrace(); }
        if (failure != null) failure.printStackTrace();
        System.out.println("DEPOSIT_VISUAL " + (failure == null ? "COMPLETE" : "FAILED: " + failure));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.disconnect();
        mc.stop();
    }
}
