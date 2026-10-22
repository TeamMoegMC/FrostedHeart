package com.teammoeg.frostedheart.content.world.features;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real TWR normal-preset terrain and framebuffer screenshots in isolated new client saves. */
@Mod.EventBusSubscriber(modid = "frostedheart", value = Dist.CLIENT)
public final class IceCaveClientValidation {
    private static final long[] SEEDS = Arrays.stream(System.getProperty("frostedheart.iceCaveClientSeeds", "111,222,333")
            .split(",")).mapToLong(Long::parseLong).toArray();
    private static final int SIZE = Integer.getInteger("frostedheart.iceCaveSampleChunks", 4);
    private static final ResourceLocation ICE = new ResourceLocation("the_winter_rescue", "ice_caves");
    private static final long START = System.nanoTime();
    private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
    private static volatile int stage;
    private static volatile boolean pending, finished;
    private static int seedIndex, chunkIndex, phase, attempt, cameraIndex, frames;
    private static int startX, startZ;
    private static long cameraReady;
    private static String lastScreenState = "";
    private static List<BlockPos> candidates;
    private static IceCaveLandscapeMetrics metrics;
    private static Map<String, Object> result;
    private static List<Map<String, Object>> cameras;
    private static final Path OUTPUT = Path.of(System.getProperty("frostedheart.validationRoot", ".."), "build", "ice-cave-client");

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("frostedheart.iceCaveClientValidation") || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.options.pauseOnLostFocus = false;
            String screenState = "stage=" + stage + " screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getName())
                    + " overlay=" + (mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getName());
            if (!screenState.equals(lastScreenState)) {
                lastScreenState = screenState;
                System.out.println("ICE_CAVE_CLIENT state " + screenState);
            }
            if ((System.nanoTime() - START) / 1_000_000_000L > Integer.getInteger("frostedheart.iceCaveClientTimeoutSeconds", 1800))
                throw new IllegalStateException("Client sampling timed out at seed=" + seedIndex + " stage=" + stage + " phase=" + phase);
            if (stage > 0 && mc.screen instanceof AlertScreen alert)
                throw new IllegalStateException("World startup/capture AlertScreen: " + alert.getNarrationMessage().getString());
            if (stage == 5) {
                // Follow vanilla PauseScreen: close the owner's connection before clearLevel waits for the server.
                // Stage 6 keeps nested ticks/renders during clearLevel out of this transition.
                stage = 6;
                if (mc.level != null) mc.level.disconnect();
                mc.clearLevel();
                mc.setScreen(new TitleScreen());
                if (seedIndex == SEEDS.length) finish(null);
                else stage = 0;
                return;
            }
            if (stage == 0 && mc.getOverlay() == null && mc.screen instanceof AccessibilityOnboardingScreen onboarding) {
                // This opt-in client has its own gameDir and options; use vanilla's normal close path.
                onboarding.onClose();
                return;
            }
            if (stage == 0 && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
                if (seedIndex == SEEDS.length) { finish(null); return; }
                stage = 1;
                String name = "ice-cave-v2-normal-" + SEEDS[seedIndex] + "-" + System.currentTimeMillis();
                result = new LinkedHashMap<>();
                result.put("seed", SEEDS[seedIndex]); result.put("save_directory", "build/ice-cave-client-runtime/saves/" + name);
                result.put("preset", "minecraft:normal, unmodified TWR multi_noise biome source and terrain/features");
                result.put("resource_adaptations", OUTPUT.getParent().resolve("ice-cave-normal-assets-adaptations.json").toAbsolutePath().toString());
                result.put("runtime", "Spectator + night vision; randomTickSpeed=0 and weather/mobs disabled for fresh-generation visual capture; no terrain edit commands");
                System.out.println("ICE_CAVE_CLIENT creating seed=" + SEEDS[seedIndex] + " save=" + name);
                mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings(name, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(SEEDS[seedIndex], true, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            }
            if (stage == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && !pending) {
                stage = 2;
                submit(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, mc.getSingleplayerServer());
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, mc.getSingleplayerServer());
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, mc.getSingleplayerServer());
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
                    level.setWeatherParameters(0, 12000, false, false);
                    level.setDayTime(6000);
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 72000, 0, false, false));
                    candidates = findCandidates(level);
                    result.put("biome_search_candidates", candidates.stream().map(p -> List.of(p.getX(), p.getY(), p.getZ())).toList());
                    attempt = 0; beginPatch();
                });
            } else if (stage == 2 && !pending) submit(IceCaveClientValidation::sampleStep);
            else if (stage == 4 && mc.player != null) {
                Map<String, Object> view = cameras.get(cameraIndex);
                mc.player.setYRot(((Number) view.get("yaw")).floatValue());
                mc.player.setXRot(((Number) view.get("pitch")).floatValue());
                mc.options.hideGui = true;
                mc.options.renderDistance().set(7);
                mc.options.simulationDistance().set(5);
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void submit(Runnable work) {
        pending = true;
        Minecraft mc = Minecraft.getInstance();
        mc.getSingleplayerServer().execute(() -> {
            try { work.run(); }
            catch (Throwable failure) { mc.execute(() -> finish(failure)); }
            finally { pending = false; }
        });
    }

    private static List<BlockPos> findCandidates(ServerLevel level) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        List<BlockPos> positions = new ArrayList<>();
        // Query only climate, without forcing terrain chunks. Candidate centers are separated by >=256 blocks.
        for (int radius = 0; radius <= 8192 && positions.size() < 8; radius += 128)
            for (int dx = -radius; dx <= radius && positions.size() < 8; dx += 128)
                for (int dz = -radius; dz <= radius && positions.size() < 8; dz += 128) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = 1024 + dx, z = 1024 + dz;
                    for (int y : new int[] {-32, -16, 0, 24}) {
                        if (!source.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler).is(ICE)) continue;
                        BlockPos pos = new BlockPos(x, y, z);
                        if (positions.stream().noneMatch(other -> Math.abs(other.getX() - x) < 256 && Math.abs(other.getZ() - z) < 256))
                            positions.add(pos);
                        break;
                    }
                }
        if (positions.isEmpty()) throw new IllegalStateException("No normal-preset ice-cave climate candidate within finite 8192-block search");
        return positions;
    }

    private static void beginPatch() {
        BlockPos center = candidates.get(attempt);
        startX = (center.getX() >> 4) - SIZE / 2; startZ = (center.getZ() >> 4) - SIZE / 2;
        chunkIndex = 0; phase = 0; metrics = new IceCaveLandscapeMetrics();
        System.out.println("ICE_CAVE_CLIENT patch seed=" + SEEDS[seedIndex] + " attempt=" + attempt + " chunk=" + startX + "," + startZ);
    }

    private static void sampleStep() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().overworld();
        Predicate<BlockPos> include = pos -> level.getBiome(pos).is(ICE);
        int width = SIZE + 2;
        if (phase == 0) level.getChunkSource().getChunk(startX - 1 + chunkIndex % width, startZ - 1 + chunkIndex / width, ChunkStatus.CARVERS, true);
        else if (phase == 1) metrics.beforeChunk(level, startX + chunkIndex % SIZE, startZ + chunkIndex / SIZE, include);
        else if (phase == 2) level.getChunk(startX - 1 + chunkIndex % width, startZ - 1 + chunkIndex / width);
        else if (phase == 3) metrics.afterChunk(level, startX + chunkIndex % SIZE, startZ + chunkIndex / SIZE, include);
        if (++chunkIndex < (phase == 0 || phase == 2 ? width * width : SIZE * SIZE)) return;
        chunkIndex = 0;
        if (++phase < 4) return;
        if (metrics.views().stream().noneMatch(view -> view.get("kind").equals("hall")) && attempt + 1 < Math.min(4, candidates.size())) {
            attempt++; beginPatch(); return;
        }
        result.put("generated_patch_attempts", attempt + 1);
        result.put("hall_observation_found", metrics.views().stream().anyMatch(view -> view.get("kind").equals("hall")));
        result.put("interior_chunk_min_x", startX); result.put("interior_chunk_min_z", startZ);
        result.put("interior_chunks", SIZE * SIZE); result.put("generated_border_chunks", width * width - SIZE * SIZE);
        result.put("landscape", metrics.finish(level));
        cameras = new ArrayList<>();
        for (Map<String, Object> source : metrics.views()) {
            Map<String, Object> view = new LinkedHashMap<>(source); cameras.add(view);
            if (source.get("kind").equals("hall")) {
                Map<String, Object> reverse = new LinkedHashMap<>(source);
                reverse.put("kind", "hall_alternate"); reverse.put("yaw", source.get("alternate_yaw")); cameras.add(reverse);
            }
        }
        if (cameras.isEmpty()) throw new IllegalStateException("Normal-preset candidates contained no walkable cave observation point");
        result.put("screenshots", cameras);
        cameraIndex = 0; teleport();
    }

    private static void teleport() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        Map<String, Object> view = cameras.get(cameraIndex);
        player.teleportTo(mc.getSingleplayerServer().overworld(), ((Number) view.get("x")).doubleValue(),
                ((Number) view.get("y")).doubleValue() + .05, ((Number) view.get("z")).doubleValue(),
                ((Number) view.get("yaw")).floatValue(), ((Number) view.get("pitch")).floatValue());
        frames = 0; cameraReady = System.nanoTime(); stage = 4;
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!Boolean.getBoolean("frostedheart.iceCaveClientValidation") || finished || stage != 4
                || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getOverlay() != null || mc.screen != null) return;
        Map<String, Object> view = cameras.get(cameraIndex);
        if (mc.player.distanceToSqr(((Number) view.get("x")).doubleValue(), ((Number) view.get("y")).doubleValue(),
                ((Number) view.get("z")).doubleValue()) > 4 || !mc.level.hasChunkAt(mc.player.blockPosition())) return;
        if (++frames < 160 || (System.nanoTime() - cameraReady) < 12_000_000_000L) return;
        try {
            Files.createDirectories(OUTPUT);
            Path image = OUTPUT.resolve("seed-" + SEEDS[seedIndex] + "-" + view.get("kind") + ".png");
            try (var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { screenshot.writeToFile(image); }
            view.put("image", image.toAbsolutePath().toString());
            view.put("image_width", mc.getMainRenderTarget().width);
            view.put("image_height", mc.getMainRenderTarget().height);
            System.out.println("ICE_CAVE_CLIENT screenshot " + image + " view=" + view);
            stage = 3;
            if (++cameraIndex < cameras.size()) submit(IceCaveClientValidation::teleport);
            else {
                RESULTS.add(result); writeResults(); seedIndex++;
                stage = 5;
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void writeResults() throws Exception {
        Files.createDirectories(OUTPUT);
        Files.writeString(OUTPUT.resolve("normal-preset-samples.json"), new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(RESULTS));
    }
    private static void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        try {
            if (failure != null && result != null && !RESULTS.contains(result)) {
                result.put("failure", failure.toString()); result.put("client_state_at_failure", lastScreenState);
                RESULTS.add(result);
            }
            writeResults(); Files.writeString(OUTPUT.resolve("result.txt"), failure == null ? "CAPTURE COMPLETE; visual acceptance requires inspecting the PNG files.\n"
                    : "FAILED: " + failure + "\n");
        } catch (Exception writeFailure) { writeFailure.printStackTrace(); }
        if (failure != null) failure.printStackTrace();
        System.out.println("ICE_CAVE_CLIENT " + (failure == null ? "CAPTURE COMPLETE" : "FAILED: " + failure));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.disconnect();
        mc.stop();
    }
}
