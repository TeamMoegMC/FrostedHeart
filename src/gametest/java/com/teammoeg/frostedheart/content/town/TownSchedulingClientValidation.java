package com.teammoeg.frostedheart.content.town;

import com.google.gson.GsonBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.item.townmanager.TownHousingPanel;
import com.teammoeg.frostedheart.item.townmanager.TownManagerScreen;
import com.teammoeg.frostedheart.item.townmanager.TownStaffingPanel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
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

/** Opt-in real GL client, integrated server, normal management packets and mouse input. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public final class TownSchedulingClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedheart.townSchedulingValidation");
    private static final Path OUTPUT = Path.of(System.getProperty("frostedheart.townSchedulingRoot", "."))
            .resolve(System.getProperty("frostedheart.townSchedulingOutput", "build/town-scheduling-validation"));
    private static final long START = System.nanoTime();
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static boolean worldRequested, sceneRequested, finished;
    private static volatile TownViewSyncGameTests.Session session;
    private static volatile boolean ready, actionDone;
    private static volatile Throwable serverFailure;
    private static volatile Map<String, Object> serverSnapshot;
    private static int step, ticks, frames;
    private static long baselineBatch;
    private static TownManagerScreen manager;
    private static boolean missingQueues;
    private static BlockPos extraHouse;

    private TownSchedulingClientValidation() {}

    @SubscribeEvent
    public static void client(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            require(System.nanoTime() - START < 600_000_000_000L, "rendered scheduling validation timed out at step " + step);
            if (serverFailure != null) throw new IllegalStateException("server scenario failed", serverFailure);
            if (!worldRequested && mc.getOverlay() == null && mc.screen != null && mc.level == null) {
                worldRequested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.guiScale().set(2);
                mc.options.framerateLimit().set(60);
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("town-scheduling-" + System.currentTimeMillis(),
                        new LevelSettings("Town scheduling validation", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                true, rules, WorldDataConfiguration.DEFAULT),
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
                        session = new TownViewSyncGameTests.Session(server.overworld(), new BlockPos(0, 65, 0),
                                server.getPlayerList().getPlayer(mc.player.getUUID()));
                        session.first.closeContainer();
                        session.second.closeContainer();
                        session.level.destroyBlock(session.housePos, false);
                        session.level.destroyBlock(session.warehousePos, false);
                        session.first.teleportTo(session.level, 3.5, 66, 3.5, 0, 0);
                        session.warm();
                        ready = true;
                    } catch (Throwable failure) { serverFailure = failure; }
                });
            }
            if (!ready || mc.getOverlay() != null || TownClientUpdates.currentTown().isEmpty()) return;
            TeamTownData client = TownClientUpdates.currentTown().orElseThrow();
            ticks++;
            switch (step) {
                case 0 -> {
                    if (ticks < 30) return;
                    require(client.buildings.isEmpty(), "initial full baseline must be an empty town");
                    baselineBatch = TeamTownData.getClientSyncBatchId();
                    manager = new TownManagerScreen(4);
                    CUIScreenWrapper.open(manager);
                    next();
                }
                case 1 -> {
                    if (ticks < 20) return;
                    capture(mc, "00-empty-staffing.png");
                    action(() -> {
                        TownSchedulingGameTests.populate(session);
                        TownSchedulingGameTests.settle(session);
                    });
                    next();
                }
                case 2 -> {
                    if (!actionDone || ticks < 60) return;
                    require(client.buildings.size() == 3 && client.residents.size() == 3,
                            "actual building and resident packets must reach the client");
                    require(TeamTownData.getClientSyncBatchId() == baselineBatch,
                            "ordinary construction must use deltas without a replacement full view");
                    RESULTS.put("zeroGuaranteeServer", serverSnapshot);
                    RESULTS.put("zeroGuaranteeClient", snapshot(client));
                    capture(mc, "01-zero-guarantee-staffing.png");
                    missingQueues = client.getStaffingPlan().entries().isEmpty() || client.getHousingPlan().entries().isEmpty();
                    require(client.residents.values().stream().allMatch(r -> r.getWorkPos() != null && r.getHousePos() != null),
                            "all three residents must have actual jobs and homes on the client");
                    manager.selectTab(5);
                    next();
                }
                case 3 -> {
                    if (ticks < 20) return;
                    capture(mc, "02-zero-guarantee-housing.png");
                    require(!missingQueues, "confirmed: server has housing/jobs but both client scheduling queues remained empty after building deltas");
                    require(client.getStaffingPlan().target(session.extraPos) == 0
                                    && client.getHousingPlan().guaranteedResidents(session.housePos) == 0,
                            "zero targets represent guarantees and do not block assignment");
                    manager.selectTab(4);
                    next();
                }
                case 4 -> {
                    if (ticks < 10) return;
                    int capacity = ((com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding)
                            client.buildings.get(session.extraPos)).getMaxResidents();
                    clickTarget(mc, TownStaffingPanel.class, 128, 2, capacity);
                    next();
                }
                case 5 -> {
                    if (ticks < 25) return;
                    require(client.getStaffingPlan().target(session.extraPos) == 2,
                            "clicking the actual staffing slider must round-trip through the server to two guaranteed workers");
                    capture(mc, "03-staffing-guarantee-two.png");
                    manager.selectTab(5);
                    next();
                }
                case 6 -> {
                    if (ticks < 10) return;
                    int capacity = ((HouseBuilding) client.buildings.get(session.housePos)).getMaxResidents();
                    clickTarget(mc, TownHousingPanel.class, 138, 2, capacity);
                    next();
                }
                case 7 -> {
                    if (ticks < 25) return;
                    require(client.getHousingPlan().guaranteedResidents(session.housePos) == 2,
                            "clicking the actual housing slider must round-trip through the server to two guaranteed meals");
                    capture(mc, "04-housing-guarantee-two.png");
                    action(() -> {
                        require(session.data.getStaffingPlan().target(session.extraPos) == 2
                                        && session.data.getHousingPlan().guaranteedResidents(session.housePos) == 2,
                                "both client mouse edits must reach server authority");
                        TownSchedulingGameTests.settle(session);
                        require(session.data.residents.values().stream().allMatch(r -> session.extraPos.equals(r.getWorkPos())
                                        && session.housePos.equals(r.getHousePos())),
                                "two guarantees must still allow three actual jobs and occupants on the next day");
                        require(((HouseBuilding) session.data.buildings.get(session.housePos)).getDailyReport().foodConsumed() > 0,
                                "next-day meal settlement must consume real warehouse food");
                    });
                    next();
                }
                case 8 -> {
                    if (!actionDone || ticks < 30) return;
                    RESULTS.put("editedGuaranteesNextDayServer", serverSnapshot);
                    RESULTS.put("editedGuaranteesNextDayClient", snapshot(client));
                    require(client.getStaffingPlan().target(session.extraPos) == 2
                                    && client.getHousingPlan().guaranteedResidents(session.housePos) == 2,
                            "daily building updates must preserve both authored targets");
                    action(() -> {
                        BlockPos room = session.origin.offset(30, 0, 0);
                        TownSchedulingGameTests.buildRoom(session, room, true);
                        extraHouse = room.offset(3, 0, 3);
                        var core = (HouseBlockEntity) session.place(extraHouse, FHBlocks.HOUSE.get());
                        core.refresh_safe(core.getBuilding().orElseThrow());
                    });
                    next();
                }
                case 9 -> {
                    if (!actionDone || ticks < 30) return;
                    require(client.getHousingPlan().entries().size() == 2, "a second house must appear while the original housing screen stays open");
                    capture(mc, "05-second-house-added.png");
                    action(() -> session.level.destroyBlock(extraHouse, false));
                    next();
                }
                case 10 -> {
                    if (!actionDone || ticks < 30) return;
                    require(client.getHousingPlan().entries().size() == 1
                                    && client.getHousingPlan().guaranteedResidents(session.housePos) == 2,
                            "demolition must remove the row and retain the other house's guarantee");
                    action(() -> {
                        session.level.destroyBlock(session.extraPos, false);
                        var core = (HouseBlockEntity) session.place(session.extraPos, FHBlocks.HOUSE.get());
                        core.refresh_safe(core.getBuilding().orElseThrow());
                    });
                    next();
                }
                case 11 -> {
                    if (!actionDone || ticks < 30) return;
                    require(client.getStaffingPlan().entries().isEmpty() && client.getHousingPlan().entries().size() == 2,
                            "replacing the hunting core with a house must update both queues at the same position");
                    capture(mc, "06-core-type-replaced.png");
                    RESULTS.put("replacementClient", snapshot(client));
                    require(TeamTownData.getClientSyncBatchId() == baselineBatch,
                            "all construction, demolition, edits and settlements must finish without a full-view workaround");
                    require(frames > 100, "the real management screen must render throughout the scenario");
                    RESULTS.put("editorReplyChecks", com.teammoeg.frostedheart.item.townmanager.TownEditorClientChecks.verify(manager));
                    finish(null);
                }
                default -> throw new AssertionError("unknown step " + step);
            }
        } catch (Throwable failure) { finish(failure); }
    }

    @SubscribeEvent
    public static void server(TickEvent.ServerTickEvent event) {
        if (!ENABLED || finished || !ready || event.phase != TickEvent.Phase.END
                || event.getServer() != session.level.getServer()) return;
        // Keep unrelated weather out of this queue/assignment check, using real building setters and deltas.
        TownSchedulingGameTests.temperate(session);
        session.drain();
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (ENABLED && !finished && manager != null && event.phase == TickEvent.Phase.END
                && Minecraft.getInstance().screen instanceof CUIScreen cui && cui.getPrimaryLayer() == manager) frames++;
    }

    private static void next() { step++; ticks = 0; }

    private static void action(Runnable work) {
        actionDone = false;
        Minecraft.getInstance().getSingleplayerServer().execute(() -> {
            try {
                work.run();
                session.data.tick(session.level, session.holder);
                serverSnapshot = snapshot(session.data);
                actionDone = true;
            } catch (Throwable failure) { serverFailure = failure; }
        });
    }

    private static Map<String, Object> snapshot(TeamTownData data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildings", data.buildings.size());
        result.put("staffingRows", data.getStaffingPlan().entries().size());
        result.put("housingRows", data.getHousingPlan().entries().size());
        result.put("guaranteedWorkers", data.getStaffingPlan().entries().stream().mapToInt(TownStaffingPlan.Entry::targetWorkers).sum());
        result.put("guaranteedMeals", data.getHousingPlan().entries().stream().mapToInt(TownHousingPlan.Entry::guaranteedResidents).sum());
        result.put("population", data.residents.size());
        result.put("employed", data.residents.values().stream().filter(r -> r.getWorkPos() != null).count());
        result.put("housed", data.residents.values().stream().filter(r -> r.getHousePos() != null).count());
        result.put("idle", data.residents.values().stream().filter(r -> r.getWorkPos() == null).count());
        result.put("foodConsumed", data.buildings.values().stream().filter(HouseBuilding.class::isInstance)
                .map(HouseBuilding.class::cast).mapToDouble(h -> h.getDailyReport().foodConsumed()).sum());
        return result;
    }

    private static <T extends UIElement> T find(UIElement element, Class<T> type) {
        if (type.isInstance(element)) return type.cast(element);
        if (element instanceof UILayer layer) for (UIElement child : layer.getElements()) {
            T found = find(child, type);
            if (found != null) return found;
        }
        return null;
    }

    private static void clickTarget(Minecraft mc, Class<? extends UIElement> type, int barX, int target, int capacity) {
        UIElement panel = find(manager, type);
        require(panel != null && capacity > 0, "the actual target slider must exist");
        int originX = (mc.screen.width - manager.getWidth()) / 2;
        int originY = (mc.screen.height - manager.getHeight()) / 2;
        double x = originX + panel.getScreenX() + barX
                + (TownManagerScreen.CONTENT_WIDTH - barX - 8) * target / (double) capacity;
        double y = originY + panel.getScreenY() + 32 + 19;
        // The wrapper centers its primary layer externally; update the hover state as its render does.
        manager.updateGui(originX, originY, x, y, -1);
        manager.updateMouseOver();
        require(mc.screen.mouseClicked(x, y, 0), "normal screen mouse dispatch must accept the target click");
        mc.screen.mouseReleased(x, y, 0);
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
            RESULTS.put("passed", failure == null);
            RESULTS.put("failure", failure == null ? null : failure.toString());
            RESULTS.put("lastStep", step);
            RESULTS.put("renderedFrames", frames);
            RESULTS.put("controlledBuildingTemperatureCelsius", 20);
            RESULTS.put("extraFullViewInstalls", TeamTownData.getClientSyncBatchId() - baselineBatch);
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve("result.json"), new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(RESULTS));
            FHMain.LOGGER.info("TOWN_SCHEDULING_CLIENT_RESULT {}", RESULTS);
        } catch (Exception writeFailure) { writeFailure.printStackTrace(); }
        if (failure != null) failure.printStackTrace();
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        if (mc.getSingleplayerServer() != null && session != null) {
            mc.getSingleplayerServer().execute(() -> { session.close(); mc.execute(mc::stop); });
        } else mc.stop();
    }
}
