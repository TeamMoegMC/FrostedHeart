package com.teammoeg.frostedheart.content.town;

import com.google.gson.GsonBuilder;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Opt-in real rendering and local network delivery; the second server player uses an EmbeddedChannel. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public final class TownViewClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedheart.townViewValidation");
    private static final Path OUTPUT = Path.of(System.getProperty("frostedheart.townViewValidationRoot", "."),
            "build", "town-view-client-20261107");
    private static final long STARTED = System.nanoTime();
    private static final int STAGE_TICKS = 600;
    private static int warmupTicks;
    private static int openedStage = -1;
    private static boolean worldRequested, sceneRequested, finished;
    private static volatile TownViewSyncGameTests.Session session;
    private static volatile Throwable serverFailure;
    private static volatile boolean sceneReady, serverDone;
    private static volatile int serverStage = -1, stageTick;
    private static final int[] frames = new int[3];
    private static final long[] batches = {-1, -1, -1};
    private static final Object[] screens = new Object[3];
    private static int blankFrames, deposits;
    private static int lastCapturedStage = -1;
    private static boolean waitingCaptured, recoveredCaptured;

    private TownViewClientValidation() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            require(System.nanoTime() - STARTED < 600_000_000_000L, "rendered town-view validation timed out");
            if (serverFailure != null) throw new IllegalStateException("server actions failed", serverFailure);
            if (!worldRequested && mc.getOverlay() == null && mc.screen != null && mc.level == null) {
                worldRequested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.guiScale().set(2);
                mc.options.framerateLimit().set(60);
                mc.createWorldOpenFlows().createFreshLevel("town-view-" + System.currentTimeMillis(),
                        new LevelSettings("Town view validation", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(755111011717574879L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            }
            if (!sceneRequested && mc.screen == null && mc.getOverlay() == null
                    && mc.player != null && mc.getSingleplayerServer() != null) {
                sceneRequested = true;
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        var server = mc.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                        session = new TownViewSyncGameTests.Session(server.overworld(), new BlockPos(0, 65, 0), player);
                        player.teleportTo(server.overworld(), 3.5, 66, 3.5, 0, 0);
                        session.warm();
                        sceneReady = true;
                    } catch (Throwable failure) { serverFailure = failure; }
                });
            }
            int requestedStage = serverStage;
            if (sceneReady && requestedStage >= 0 && requestedStage < 3 && openedStage != requestedStage && mc.level != null) {
                var core = mc.level.getBlockEntity(session.housePos);
                Class<?> expected = requestedStage == 0
                        ? com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockEntity.class
                        : requestedStage == 1 ? com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBlockEntity.class
                        : com.teammoeg.frostedheart.content.town.buildings.mine.MineBlockEntity.class;
                if (expected.isInstance(core)
                        && ((com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity<?>) core).getBuilding().isPresent()) {
                    openedStage = requestedStage;
                    mc.getSingleplayerServer().execute(() -> NetworkHooks.openScreen(session.first,
                            (MenuProvider) session.level.getBlockEntity(session.housePos), session.housePos));
                }
            }
            if (serverDone) {
                for (int stage = 0; stage < 3; stage++) require(frames[stage] > 30,
                        "each actual building screen must render at least thirty checked frames");
                require(blankFrames == 0, "no checked frame may lose its town or building");
                finish(null);
            }
        } catch (Throwable failure) { finish(failure); }
    }

    @SubscribeEvent
    public static void server(TickEvent.ServerTickEvent event) {
        if (!ENABLED || finished || !sceneReady || serverDone || event.phase != TickEvent.Phase.END) return;
        try {
            var current = session;
            if (event.getServer() != current.level.getServer()) return;
            if (++warmupTicks <= 40) return;
            int elapsed = ++current.elapsed;
            int stage = (elapsed - 1) / STAGE_TICKS;
            int local = (elapsed - 1) % STAGE_TICKS + 1;
            if (stage >= 3) {
                current.drain();
                require(current.secondProbe.full == 0, "another player's ordinary operations must never resync the whole town");
                require(current.secondProbe.resources > 0 && current.secondProbe.buildings > 0,
                        "the second real server connection must receive both delta types");
                serverDone = true;
                return;
            }
            serverStage = stage;
            stageTick = local;
            if (local == 1) {
                current.first.closeContainer();
                if (stage == 1) current.place(current.housePos, FHBlocks.HUNTING_BASE.get());
                if (stage == 2) current.place(current.housePos, FHBlocks.MINE.get());
            }
            if (local >= 40 && local <= 160 && local % 20 == 0) { current.deposit(); deposits++; }
            if (local == 60) current.placeExtra();
            if (local == 100) current.removeExtra();
            if (local == 70 || local == 150) {
                current.second.closeContainer();
                NetworkHooks.openScreen(current.second,
                        (MenuProvider) current.level.getBlockEntity(current.warehousePos), current.warehousePos);
            }
            if (stage == 0 && local == 120) {
                current.data.inSettlement = true;
                for (int i = 0; i < 12; i++) TownSync.requestFull(current.first, "rendered_recovery");
            }
            if (stage == 0 && local == 145) current.data.inSettlement = false;
            current.drain();
        } catch (Throwable failure) { serverFailure = failure; }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || !sceneReady || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        int stage = serverStage, local = stageTick;
        if (stage < 0 || stage >= 3 || local < 25 || local > STAGE_TICKS - 10 || mc.getOverlay() != null) return;
        try {
            if (!(mc.screen instanceof CUIScreen cui)
                    || !(cui.getPrimaryLayer() instanceof AbstractTownWorkerBlockScreen<?> screen)) {
                blankFrames++;
                throw new AssertionError("the expected building screen disappeared at stage " + stage + ", tick " + local);
            }
            if (screens[stage] == null) {
                screens[stage] = mc.screen;
                batches[stage] = TeamTownData.getClientSyncBatchId();
            }
            require(screens[stage] == mc.screen, "stock and recovery must retain the same screen instance");
            if (TownClientUpdates.currentTown().isEmpty() || screen.getCBEMenu().getBlock().getBuilding().isEmpty()) {
                blankFrames++;
                throw new AssertionError("the rendered building lost its data at stage " + stage + ", tick " + local);
            }
            long fullInstalls = TeamTownData.getClientSyncBatchId() - batches[stage];
            require(fullInstalls <= (stage == 0 ? 1 : 0), "ordinary GUI operations must not reinstall complete views");
            if (stage == 0 && local >= 165) require(fullInstalls == 1, "the deliberate pending refresh must install once");
            frames[stage]++;
            if (local >= 50 && lastCapturedStage < stage) {
                capture(mc, "stage-" + stage + "-stable.png");
                lastCapturedStage = stage;
            }
            if (stage == 0 && local >= 130 && local <= 140 && !waitingCaptured) {
                capture(mc, "house-pending-retained.png"); waitingCaptured = true;
            }
            if (stage == 0 && local >= 170 && !recoveredCaptured) {
                capture(mc, "house-recovered.png"); recoveredCaptured = true;
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Files.createDirectories(OUTPUT);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUTPUT.resolve(name)); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("passed", failure == null);
            result.put("failure", failure == null ? null : failure.toString());
            result.put("checkedFrames", frames);
            result.put("blankFrames", blankFrames);
            result.put("deposits", deposits);
            result.put("pendingScreenshot", waitingCaptured);
            result.put("recoveredScreenshot", recoveredCaptured);
            if (session != null) {
                result.put("secondPlayerFullEnvelopes", session.secondProbe.full);
                result.put("secondPlayerResourceDeltas", session.secondProbe.resources);
                result.put("secondPlayerBuildingDeltas", session.secondProbe.buildings);
            }
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve("result.json"), new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(result));
            FHMain.LOGGER.info("TOWN_VIEW_CLIENT_RESULT {}", result);
        } catch (Exception writeFailure) { writeFailure.printStackTrace(); }
        if (failure != null) failure.printStackTrace();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null && session != null) {
            mc.getSingleplayerServer().execute(() -> {
                session.close();
                mc.execute(mc::stop);
            });
        } else mc.stop();
    }
}
