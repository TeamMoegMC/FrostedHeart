/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */
package com.teammoeg.frostedresearch.gui.archive;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PanZoomViewport.Camera;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayoutEngine;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Opt-in real-client route: a fresh world, the normal drawing-desk menu, rendering and screen input. */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class ResearchGraphClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedresearch.uiValidation");
    private static final Logger LOGGER = LoggerFactory.getLogger(ResearchGraphClientValidation.class);
    private static final long STARTED = System.nanoTime();
    private static final String RUN = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    private static final String WORLD = "research-ui-validation-" + RUN;
    private static final Path OUTPUT = Path.of(System.getProperty("frostedresearch.uiValidationRoot", "."),
            "build", "research-ui-validation");
    private static final BlockPos DESK = new BlockPos(0, 65, 0);
    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("zh-overview", "zh_cn", 1280, 800, 2, .15),
            new Scenario("zh-continuity-29", "zh_cn", 1280, 800, 2, .29),
            new Scenario("zh-continuity-30", "zh_cn", 1280, 800, 2, .30),
            new Scenario("zh-continuity-37", "zh_cn", 1280, 800, 2, .37),
            new Scenario("zh-continuity-38", "zh_cn", 1280, 800, 2, .38),
            new Scenario("zh-icons", "zh_cn", 1280, 800, 2, .45),
            new Scenario("zh-cards", "zh_cn", 1280, 800, 2, 1),
            new Scenario("zh-large", "zh_cn", 1280, 800, 2, 1.75),
            new Scenario("zh-narrow-overview", "zh_cn", 640, 480, 2, .15),
            new Scenario("zh-narrow-icons", "zh_cn", 640, 480, 2, .45),
            new Scenario("zh-narrow-cards", "zh_cn", 640, 480, 2, 1),
            new Scenario("zh-scale3-overview", "zh_cn", 1280, 960, 3, .15),
            new Scenario("zh-scale1-overview", "zh_cn", 1280, 800, 1, .15),
            new Scenario("en-overview", "en_us", 1280, 800, 2, .15),
            new Scenario("en-compact", "en_us", 1280, 800, 2, .45),
            new Scenario("en-cards", "en_us", 1280, 800, 2, 1));

    private static final List<Map<String, Object>> observations = new ArrayList<>();
    private static boolean worldRequested;
    private static boolean sceneRequested;
    private static volatile boolean sceneReady;
    private static boolean interactionSent;
    private static boolean configured;
    private static boolean finished;
    private static volatile Throwable serverFailure;
    private static int scenarioIndex;
    private static int frame;
    private static int action;
    private static int geometryBuilds;
    private static int titleBuilds;
    private static int projectionBuilds;
    private static int readyTicks;
    private static int menuWaitTicks;
    private static int originalScale;
    private static int originalFrameLimit;
    private static boolean originalPause;
    private static boolean originalEditor;
    private static String originalLanguage;
    private static String selectedId;
    private static String translationId;
    private static DrawDeskScreen desk;
    private static ResearchArchiveLayer archive;
    private static ResearchGraphViewport graph;
    private static ResearchWorkspaceState state;
    private static Camera savedCamera;
    private static Camera pannedCamera;
    private static int repeatedPanTitleRebuilds;
    private static Set<String> beforeFarPanIds;
    private static int farPanNewNodes;
    private static int farPanReturnTitleRebuilds;
    private static CompletableFuture<Void> resourceReload;
    private static Language beforeResourceReloadLanguage;
    private static String resourceReloadLanguageCode;
    private static long beforeResourceReloadRevision;
    private static int resourceReloadTitleRebuilds;
    private static boolean sameLanguageReloadPassed;
    private static double pointerX;
    private static double pointerY;
    private static double wheelWorldX;
    private static double wheelWorldY;
    private static double wheelTargetZoom;
    private static boolean wheelPending;
    private static boolean intermediateZoomObserved;
    private static int zoomFrames;
    private static Method mouseMove;
    private static Map<String, PanNode> panReference;
    private static NativeImage panReferenceImage;
    private static PanNode panTitle;
    private static int panTitleWidth;
    private static int panTitleHeight;
    private static Camera panCamera;
    private static int stablePanFrames;
    private static long comparedTextPixels;
    private static Object staticPlanKey;
    private static int staticProjectionBuilds;
    private static int staticObservationRestarts;

    private record PanNode(int x, int y, int iconX, int iconY, int fontX, int fontY,
                           int width, int height, String first, String second) {}

    private record Scenario(String name, String language, int width, int height, int guiScale, double zoom) {}

    private enum Interaction {
        RESIZE, PREPARE_LOW_ZOOM, HOVER_PROJECT, OPEN_PROJECT, CLOSE_PROJECT, OPEN_TITLE, CLOSE_TITLE, FOCUS_PROJECT,
        PREPARE_WHEEL, START_WHEEL, RUN_WHEEL, CHECK_WHEEL_AND_PAN,
        PAN, RETURN_PAN, REPEAT_PAN, CHECK_PAN_CACHE,
        FAR_PAN, CHECK_FAR_PAN, CHECK_FAR_PAN_RETURN, SEARCH, CHECK_SEARCH,
        RELOAD_RESOURCES, CHECK_RESOURCE_RELOAD,
        SHOW_EMPTY_GRAPH, RESTORE_NORMAL_GRAPH, FINISH
    }

    private ResearchGraphClientValidation() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            require((System.nanoTime() - STARTED) / 1_000_000_000L < 360, "Real research UI client timed out");
            if (serverFailure != null) throw new IllegalStateException("Drawing-desk setup failed", serverFailure);
            if (!worldRequested && mc.getOverlay() == null && mc.screen != null && mc.level == null) {
                worldRequested = true;
                originalScale = mc.options.guiScale().get();
                originalFrameLimit = mc.options.framerateLimit().get();
                originalPause = mc.options.pauseOnLostFocus;
                originalLanguage = mc.options.languageCode;
                originalEditor = FHResearch.editor;
                LOGGER.info("RESEARCH_UI_OPTIONS scale={}, fps={}, pause={}, language={}",
                        originalScale, originalFrameLimit, originalPause, originalLanguage);
                mc.options.pauseOnLostFocus = false;
                mc.options.framerateLimit().set(60);
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings("Research graph validation", GameType.CREATIVE, false,
                                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(755111011717574879L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            }
            if (!sceneRequested && mc.screen == null && mc.getOverlay() == null
                    && mc.player != null && mc.getSingleplayerServer() != null && ++readyTicks >= 20) {
                sceneRequested = true;
                mc.getSingleplayerServer().execute(ResearchGraphClientValidation::prepareDesk);
            }
            if (sceneReady && !interactionSent && mc.screen == null && mc.level != null
                    && mc.level.getBlockEntity(DESK) instanceof DrawingDeskTileEntity && mc.gameMode != null) {
                interactionSent = true;
                var hit = new BlockHitResult(Vec3.atCenterOf(DESK), Direction.UP, DESK, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                LOGGER.info("RESEARCH_UI_USE_DESK clientTick={}", mc.level.getGameTime());
            }
            if (interactionSent && desk == null && ++menuWaitTicks == 40) {
                LOGGER.info("RESEARCH_UI_WAIT_SCREEN screen={}, overlay={}",
                        mc.screen == null ? "world" : mc.screen.getClass().getName(),
                        mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getName());
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void prepareDesk() {
        try {
            var server = Minecraft.getInstance().getSingleplayerServer();
            var level = server.overworld();
            var player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(GameType.CREATIVE);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 3; z++) {
                level.setBlockAndUpdate(DESK.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 4; y++) level.setBlockAndUpdate(DESK.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            player.teleportTo(.5, 65, 2.5);
            ItemStack deskItem = new ItemStack(FRContents.Blocks.DRAWING_DESK.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, deskItem);
            var placement = new BlockHitResult(Vec3.atCenterOf(DESK.below()), Direction.UP, DESK.below(), false);
            require(deskItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, placement)).consumesAction(),
                    "Normal item placement must build both halves of the drawing desk");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sceneReady = true;
            LOGGER.info("RESEARCH_UI_DESK_PLACED gameTick={}", level.getGameTime());
        } catch (Throwable failure) {
            serverFailure = failure;
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || !(mc.screen instanceof CUIScreen cui)
                || !(cui.getPrimaryLayer() instanceof DrawDeskScreen currentDesk)) return;
        try {
            if (desk == null) {
                desk = currentDesk;
                FHResearch.editor = true;
                desk.showTechTree();
                return;
            }
            if (archive == null) {
                archive = child(desk, ResearchArchiveLayer.class);
                graph = child(archive, ResearchGraphViewport.class);
                state = (ResearchWorkspaceState) read(archive, "state");
                var layout = (ResearchGraphLayout) read(graph, "layout");
                require(layout.positions().size() >= 20, "Use the actual production research graph");
                selectedId = layout.positions().containsKey("mechanical_calculator") ? "mechanical_calculator"
                        : layout.positions().keySet().stream().sorted().skip(layout.positions().size() / 2).findFirst().orElseThrow();
                state.selectResearch(selectedId);
                translationId = selectedId;
                Files.createDirectories(OUTPUT.resolve(RUN));
                LOGGER.info("RESEARCH_UI_START world={}, definitions={}, output={}",
                        WORLD, layout.positions().size(), OUTPUT.resolve(RUN));
            }
            if (wheelPending) {
                double zoom = graph.getCamera().zoom();
                if (zoom > savedCamera.zoom() && zoom < wheelTargetZoom) {
                    intermediateZoomObserved = true;
                    screenshot("zoom-frame-" + String.format("%02d", zoomFrames++));
                }
                require(Math.abs(graph.screenToWorldX(pointerX - graphX()) - wheelWorldX) < 1e-7
                                && Math.abs(graph.screenToWorldY(pointerY - graphY()) - wheelWorldY) < 1e-7,
                        "Every animated frame must preserve the world point under the pointer");
            }
            if (scenarioIndex < SCENARIOS.size()) {
                runScenario(mc, SCENARIOS.get(scenarioIndex));
            } else if (++frame % 20 == 0) {
                runAction(mc);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void runScenario(Minecraft mc, Scenario scenario) throws Exception {
        if (!configured) {
            configured = true;
            frame = 0;
            GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), scenario.width, scenario.height);
            mc.options.guiScale().set(scenario.guiScale);
            mc.resizeDisplay();
            if (!mc.getLanguageManager().getSelected().equals(scenario.language)) {
                mc.getLanguageManager().setSelected(scenario.language);
                mc.options.languageCode = scenario.language;
                mc.reloadResourcePacks();
            }
            return;
        }
        frame++;
        if (frame == 20) {
            centerSelected(scenario.zoom);
            movePointer(4, mc.getWindow().getGuiScaledHeight() - 4);
        } else if (frame == 40) {
            geometryBuilds = graph.renderPlanBuildCountForTest();
            titleBuilds = graph.titlePlanBuildCountForTest();
            staticPlanKey = read(graph, "renderPlanKey");
            staticProjectionBuilds = graph.projectionBuildCountForTest();
        } else if (frame == 60) {
            if (!java.util.Objects.equals(staticPlanKey, read(graph, "renderPlanKey"))
                    || graph.projectionBuildCountForTest() != staticProjectionBuilds) {
                LOGGER.info("RESEARCH_UI_STATIC_RESTART before={}, after={}", staticPlanKey, read(graph, "renderPlanKey"));
                staticObservationRestarts++;
                frame = 39; // A resize/definition/resource change is not a static observation interval.
                return;
            }
            require(graph.renderPlanBuildCountForTest() == geometryBuilds,
                    "Unchanged static inputs rebuilt graph geometry: " + staticPlanKey);
            require(graph.titlePlanBuildCountForTest() == titleBuilds, "Static frames rebuilt node titles");
            inspect(scenario.name, scenario.zoom);
            screenshot(scenario.name);
            // Middle-button panning also works over projects in a completely filled narrow view.
            pointerX = graphX() + 8;
            pointerY = graphY() + graph.getHeight() - 8;
            movePointer(pointerX, pointerY);
        } else if (frame >= 61) {
            validatePanFrame(mc, scenario);
        }
    }

    private static void validatePanFrame(Minecraft mc, Scenario scenario) throws Exception {
        if (frame == 61) {
            panCamera = graph.getCamera();
            panReference = panNodes();
            panTitle = panReference.get(selectedId);
            Object selected = selectedNode();
            panTitleWidth = mc.font.width((net.minecraft.util.FormattedCharSequence) read(selected, "titleGlyphs1"));
            panTitleHeight = number(graph, "TITLE_TEXT_HEIGHT");
            // GUI scales 1/2 here map exactly to framebuffer pixels. Scale 3 also checks rigid geometry.
            if (scenario.guiScale <= 2) panReferenceImage = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            mc.screen.mouseClicked(pointerX, pointerY, 2);
            require(graph.isPanning(), "Middle-button input must begin real panning");
        } else {
            Map<String, PanNode> current = panNodes();
            PanNode movedTitle = current.get(selectedId);
            require(movedTitle != null, "The selected label must remain visible during the small pan");
            int dx = movedTitle.x - panTitle.x, dy = movedTitle.y - panTitle.y;
            int common = 0;
            for (var entry : current.entrySet()) {
                PanNode before = panReference.get(entry.getKey()), after = entry.getValue();
                if (before == null) continue;
                common++;
                require(after.x - before.x == dx && after.y - before.y == dy
                                && after.iconX - before.iconX == dx && after.iconY - before.iconY == dy
                                && after.fontX - before.fontX == dx && after.fontY - before.fontY == dy,
                        "Every rendered project, icon and name must move by the exact same pixel displacement");
                require(after.width == before.width && after.height == before.height
                                && after.first.equals(before.first) && after.second.equals(before.second),
                        "Panning must preserve node sizes and displayed names");
            }
            require(common >= 2, "Compare multiple projects during actual panning");
            require(graph.getCamera().zoom() == panCamera.zoom(), "Dragging must not change zoom");
            if (panReferenceImage != null) {
                int scale = scenario.guiScale;
                try (var now = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    for (int y = 0; y < panTitleHeight * scale; y++) {
                        for (int x = 0; x < panTitleWidth * scale; x++) {
                            require(panReferenceImage.getPixelRGBA(panTitle.fontX * scale + x, panTitle.fontY * scale + y)
                                            == now.getPixelRGBA(movedTitle.fontX * scale + x, movedTitle.fontY * scale + y),
                                    "The rendered title pixels must remain identical as the graph moves");
                            comparedTextPixels++;
                        }
                    }
                }
            }
            stablePanFrames++;
        }
        if (frame < 81) {
            mc.screen.mouseDragged(pointerX + .27, pointerY + .19, 2, .27, .19);
            pointerX += .27;
            pointerY += .19;
            movePointer(pointerX, pointerY);
        } else {
            mc.screen.mouseReleased(pointerX, pointerY, 2);
            require(!graph.isPanning(), "Releasing the middle button must stop panning");
            if (panReferenceImage != null) {
                panReferenceImage.close();
                panReferenceImage = null;
            }
            screenshot(scenario.name + "-panned");
            scenarioIndex++;
            configured = false;
            frame = 0;
        }
    }

    private static Map<String, PanNode> panNodes() throws Exception {
        Map<String, PanNode> nodes = new LinkedHashMap<>();
        List<?> buffer = (List<?>) read(graph, "renderedNodeBuffer");
        for (int i = 0; i < number(graph, "renderedNodeCount"); i++) {
            Object node = buffer.get(i);
            nodes.put((String) read(read(node, "source"), "id"), new PanNode(
                    number(node, "x"), number(node, "y"), number(node, "iconX"), number(node, "iconY"),
                    number(node, "fontX"), number(node, "fontY"), number(node, "width"), number(node, "height"),
                    (String) read(node, "titleLine1"), (String) read(node, "titleLine2")));
        }
        return nodes;
    }

    private static void runAction(Minecraft mc) throws Exception {
        if (Interaction.values()[action] == Interaction.CHECK_RESOURCE_RELOAD && !resourceReload.isDone()) return;
        switch (Interaction.values()[action++]) {
            case RESIZE -> {
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 800);
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
            }
            case PREPARE_LOW_ZOOM -> {
                centerSelected(.15);
                movePointer(4, 4);
            }
            case HOVER_PROJECT -> {
                Object node = selectedNode();
                pointerX = number(node, "iconX") + number(node, "iconSize") / 2d;
                pointerY = number(node, "iconY") + number(node, "iconSize") / 2d;
                movePointer(pointerX, pointerY);
                savedCamera = graph.getCamera();
                titleBuilds = graph.titlePlanBuildCountForTest();
            }
            case OPEN_PROJECT -> {
                require(graph.titlePlanBuildCountForTest() == titleBuilds, "Hover must not rebuild or move project names");
                require(graph.getCursor() == com.teammoeg.chorda.client.CInputHelper.Cursor.HAND,
                        "The minimum-zoom item's actual icon must have a usable hit target");
                click(mc);
                require(state.projectWorkspaceOpen() && selectedId.equals(state.selectedResearchId()),
                        "A minimum-zoom icon must open the intended project directly");
            }
            case CLOSE_PROJECT -> {
                screenshot("minimum-zoom-project-dialog");
                mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                require(!state.projectWorkspaceOpen() && savedCamera.equals(graph.getCamera()),
                        "Closing details must preserve the camera");
                Object node = selectedNode();
                pointerX = number(node, "fontX") + 2;
                pointerY = number(node, "fontY") + 2;
                movePointer(pointerX, pointerY);
            }
            case OPEN_TITLE -> {
                require(graph.getCursor() == com.teammoeg.chorda.client.CInputHelper.Cursor.HAND,
                        "A minimum-zoom short name must belong to its clickable project card");
                click(mc);
                require(state.projectWorkspaceOpen() && selectedId.equals(state.selectedResearchId()),
                        "Clicking the native-size short name must open its intended project");
            }
            case CLOSE_TITLE -> {
                screenshot("short-name-click-dialog");
                mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                require(!state.projectWorkspaceOpen() && savedCamera.equals(graph.getCamera()),
                        "Closing the short-name dialog must preserve the camera");
                var focus = graph.getElements().get(1);
                pointerX = graphX() + focus.getX() + focus.getWidth() / 2d;
                pointerY = graphY() + focus.getY() + focus.getHeight() / 2d;
                movePointer(pointerX, pointerY);
            }
            case FOCUS_PROJECT -> {
                click(mc);
                require(graph.getCamera().zoom() >= 1, "Focus must restore a readable zoom");
            }
            case PREPARE_WHEEL -> {
                screenshot("focused-project");
                centerSelected(.35);
            }
            case START_WHEEL -> {
                findEmptyPoint();
                movePointer(pointerX, pointerY);
                savedCamera = graph.getCamera();
                wheelWorldX = graph.screenToWorldX(pointerX - graphX());
                wheelWorldY = graph.screenToWorldY(pointerY - graphY());
                wheelTargetZoom = savedCamera.zoom() * Math.pow(graph.getZoomStep(), 3);
            }
            case RUN_WHEEL -> {
                screenshot("zoom-frame-" + String.format("%02d", zoomFrames++));
                mc.screen.mouseScrolled(pointerX, pointerY, 3);
                require(savedCamera.equals(graph.getCamera()), "Wheel input must animate rather than jump immediately");
                require(read(graph, "zoomMotion") != null, "Wheel input must schedule a camera transition");
                wheelPending = true;
            }
            case CHECK_WHEEL_AND_PAN -> {
                wheelPending = false;
                require(intermediateZoomObserved && Math.abs(graph.getCamera().zoom() - wheelTargetZoom) < 1e-7,
                        "The real client must render an intermediate zoom and settle at the requested zoom");
                require(read(graph, "zoomMotion") == null, "A settled camera must stop animating");
                screenshot("zoom-frame-" + String.format("%02d", zoomFrames++));
                screenshot("smooth-zoom-settled");
                findEmptyPoint();
                movePointer(pointerX, pointerY);
            }
            case PAN -> {
                savedCamera = graph.getCamera();
                mc.screen.mouseClicked(pointerX, pointerY, 0);
                require(graph.isPanning(), "Blank-space input must begin panning at every zoom");
                mc.screen.mouseDragged(pointerX + 12, pointerY + 8, 0, 12, 8);
                mc.screen.mouseReleased(pointerX + 12, pointerY + 8, 0);
                require(Math.abs(graph.getCamera().x() - savedCamera.x() + 12 / savedCamera.zoom()) < 1e-7
                                && Math.abs(graph.getCamera().y() - savedCamera.y() + 8 / savedCamera.zoom()) < 1e-7,
                        "Dragging must move the camera by screen distance / zoom");
                require(!state.projectWorkspaceOpen(), "Panning must not open a project");
                pannedCamera = graph.getCamera();
            }
            case RETURN_PAN -> {
                // Let both views render before repeating the drag: new arrivals may need one layout.
                graph.setCamera(savedCamera);
            }
            case REPEAT_PAN -> {
                titleBuilds = graph.titlePlanBuildCountForTest();
                geometryBuilds = graph.renderPlanBuildCountForTest();
                mc.screen.mouseClicked(pointerX, pointerY, 0);
                require(graph.isPanning(), "The restored blank-space point must still allow dragging");
                mc.screen.mouseDragged(pointerX + 12, pointerY + 8, 0, 12, 8);
                mc.screen.mouseReleased(pointerX + 12, pointerY + 8, 0);
            }
            case CHECK_PAN_CACHE -> {
                require(graph.getCamera().equals(pannedCamera), "Repeated real input must reach the same panned view");
                require(graph.renderPlanBuildCountForTest() > geometryBuilds, "The panned geometry must actually render");
                repeatedPanTitleRebuilds = graph.titlePlanBuildCountForTest() - titleBuilds;
                require(repeatedPanTitleRebuilds == 0, "Panning through warm views must reuse every node's title layout");
                inspect("pan-cache", graph.getCamera().zoom());
                screenshot("pan-cache");
            }
            case FAR_PAN -> {
                savedCamera = graph.getCamera();
                beforeFarPanIds = visibleResearchIds();
                graph.setCamera(new Camera(savedCamera.x() + ResearchGraphLayoutEngine.RANK_SPACING * 4,
                        savedCamera.y(), savedCamera.zoom()));
            }
            case CHECK_FAR_PAN -> {
                Set<String> arrived = visibleResearchIds();
                arrived.removeAll(beforeFarPanIds);
                farPanNewNodes = arrived.size();
                require(farPanNewNodes > 0, "The distant view must expose projects outside the previous viewport");
                inspect("far-pan", graph.getCamera().zoom());
                screenshot("far-pan");
                titleBuilds = graph.titlePlanBuildCountForTest();
                graph.setCamera(savedCamera);
            }
            case CHECK_FAR_PAN_RETURN -> {
                require(visibleResearchIds().equals(beforeFarPanIds), "Returning must restore the same visible projects");
                farPanReturnTitleRebuilds = graph.titlePlanBuildCountForTest() - titleBuilds;
                require(farPanReturnTitleRebuilds == 0, "Returning from a distant view must reuse the original title layouts");
                inspect("far-pan-return", graph.getCamera().zoom());
                screenshot("far-pan-return");
            }
            case SEARCH -> {
                centerSelected(.15);
                movePointer(4, 4);
                projectionBuilds = graph.projectionBuildCountForTest();
                child(archive, TextBox.class).setText(FHResearch.getResearch(selectedId).getName().getString(), true);
            }
            case CHECK_SEARCH -> {
                require(graph.projectionBuildCountForTest() == projectionBuilds, "Search must reuse the graph projection");
                inspect("search", .15);
                screenshot("search");
                child(archive, TextBox.class).setText("", true);
            }
            case RELOAD_RESOURCES -> {
                savedCamera = graph.getCamera();
                beforeResourceReloadLanguage = Language.getInstance();
                resourceReloadLanguageCode = mc.getLanguageManager().getSelected();
                beforeResourceReloadRevision = ((ResearchArchiveViewCache) read(graph, "viewCache")).presentationRevision();
                titleBuilds = graph.titlePlanBuildCountForTest();
                resourceReload = mc.reloadResourcePacks();
            }
            case CHECK_RESOURCE_RELOAD -> {
                resourceReload.join();
                require(mc.getLanguageManager().getSelected().equals(resourceReloadLanguageCode),
                        "This reload must keep the same selected language");
                require(Language.getInstance() != beforeResourceReloadLanguage,
                        "Normal resource reload must publish a fresh language/font presentation");
                long revision = ((ResearchArchiveViewCache) read(graph, "viewCache")).presentationRevision();
                require(revision > beforeResourceReloadRevision, "Same-language resource reload must invalidate title metrics");
                resourceReloadTitleRebuilds = graph.titlePlanBuildCountForTest() - titleBuilds;
                require(resourceReloadTitleRebuilds > 0, "Reloaded resources must rebuild visible title layouts");
                require(savedCamera.equals(graph.getCamera()), "Resource reload must preserve the graph camera");
                inspect("resource-reload", graph.getCamera().zoom());
                screenshot("resource-reload");
                sameLanguageReloadPassed = true;
            }
            case SHOW_EMPTY_GRAPH -> {
                state.selectResearch(null);
                graph.setDefinitions(List.of(), Long.MAX_VALUE);
                graph.setCamera(new Camera(0, 0, .15));
            }
            case RESTORE_NORMAL_GRAPH -> {
                require(number(graph, "renderedNodeCount") == 0 && number(graph, "renderedTitleCount") == 0,
                        "An empty graph must clear old icons and labels");
                ((Button) graph.getElements().get(1)).onClicked(MouseButton.LEFT);
                screenshot("empty-graph");
                FHResearch.editor = false;
                archive.onResearchDefinitionsChanged();
            }
            case FINISH -> {
                for (Object source : (List<?>) read(graph, "nodeSources")) {
                    Object view = read(source, "view");
                    Research research = (Research) read(view, "research");
                    require(!research.isHidden() && ResearchArchiveLayer.canReveal(research),
                            "Normal mode must retain the archive's existing visibility rules");
                }
                screenshot("normal-mode");
                finish(null);
            }
        }
    }

    private static void inspect(String name, double zoom) throws Exception {
        require(net.minecraft.client.resources.language.I18n.exists("research.frostedresearch." + translationId + ".name"),
                "The production catalogue's project translations must be loaded in the real client");
        String title = FHResearch.getResearch(selectedId).getName().getString();
        require(!title.isBlank(), "The actual localized project name must not disappear during component parsing");
        int nodes = number(graph, "renderedNodeCount"), names = number(graph, "renderedTitleCount");
        require(nodes > 0, "The same populated dependency graph must remain visible at every zoom");
        require(names == nodes, "Every visible project must retain a short name instead of losing the label");
        var font = Minecraft.getInstance().font;
        var titleStyle = (net.minecraft.network.chat.Style) read(graph, "TITLE_STYLE");
        int lineHeight = number(graph, "TITLE_LINE_HEIGHT");
        int cjkPairWidth = font.width(net.minecraft.util.FormattedCharSequence.forward("研究", titleStyle));
        require(titleStyle.getFont().equals(net.minecraft.network.chat.Style.DEFAULT_FONT)
                        && cjkPairWidth > 12 && cjkPairWidth <= 19,
                "Names must use the full-size native font, with room for two CJK characters at minimum zoom");
        int minIcon = 16, maxIcon = 0, twoLines = 0, fullNames = 0, characters = 0, fullyVisible = 0;
        List<?> nodeBuffer = (List<?>) read(graph, "renderedNodeBuffer");
        for (int i = 0; i < nodes; i++) {
            Object node = nodeBuffer.get(i);
            int size = number(node, "iconSize");
            require(size >= 8 && size <= 16, "Every project must retain its real item icon");
            minIcon = Math.min(minIcon, size);
            maxIcon = Math.max(maxIcon, size);
            String first = (String) read(node, "titleLine1"), second = (String) read(node, "titleLine2");
            require(!first.isBlank(), "Every project must have readable name characters next to its icon");
            if (!second.isEmpty()) twoLines++;
            String shown = (first + second).replace(" ", "");
            String original = ((ResearchArchiveViewCache.View) read(read(node, "source"), "view")).title().replace(" ", "");
            if (shown.equals(original)) fullNames++;
            String content = shown.replace("…", "");
            require(original.startsWith(content), "Cached titles must match their current localized research name");
            characters += content.codePointCount(0, content.length());
            int x = number(node, "x"), y = number(node, "y"), w = number(node, "width"), h = number(node, "height");
            if (x >= graphX() && y >= graphY() && x + w <= graphX() + graph.getWidth()
                    && y + h <= graphY() + graph.getHeight()) fullyVisible++;
            int textX = number(node, "fontX"), textY = number(node, "fontY");
            int textWidth = Math.max(font.width((net.minecraft.util.FormattedCharSequence) read(node, "titleGlyphs1")),
                    font.width((net.minecraft.util.FormattedCharSequence) read(node, "titleGlyphs2")));
            int textHeight = number(graph, "TITLE_TEXT_HEIGHT") + (second.isEmpty() ? 0 : lineHeight);
            int titleX = number(node, "titleX"), titleY = number(node, "titleY");
            int titleWidth = number(node, "titleWidth"), titleHeight = number(node, "titleHeight");
            int iconFrameSize = number(node, "iconFrameSize"), iconFrameY = number(node, "iconFrameY");
            require(iconFrameSize >= size + 2 && titleX == x + iconFrameSize - 1
                            && titleX + titleWidth == x + w
                            && iconFrameY >= y && iconFrameY + iconFrameSize <= y + h,
                    "Icon and title frames must share one border inside their project node");
            require(textX >= titleX + 1 && textX + textWidth <= titleX + titleWidth - 1
                            && textY >= titleY + 1 && textY + textHeight <= titleY + titleHeight - 1,
                    "Native-size names and wrapped lines must stay within the paper nameplate's border");
            for (int j = 0; j < i; j++) {
                Object other = nodeBuffer.get(j);
                require(!(x < number(other, "x") + number(other, "width") && x + w > number(other, "x")
                                && y < number(other, "y") + number(other, "height") && y + h > number(other, "y")),
                        "The denser project cards must preserve non-overlapping graph lanes and columns");
            }
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("scenario", name);
        row.put("selectedTitle", title);
        row.put("language", Minecraft.getInstance().getLanguageManager().getSelected());
        row.put("guiScale", Minecraft.getInstance().getWindow().getGuiScale());
        row.put("viewportWidth", graph.getWidth());
        row.put("viewportHeight", graph.getHeight());
        row.put("zoom", graph.getCamera().zoom());
        row.put("nodes", nodes);
        row.put("icons", nodes);
        row.put("minimumIconSize", minIcon);
        row.put("maximumIconSize", maxIcon);
        row.put("namedCards", names);
        row.put("fullyVisibleNamedCards", fullyVisible);
        row.put("titleFont", titleStyle.getFont().toString());
        row.put("titleLineHeight", lineHeight);
        row.put("cjkPairWidth", cjkPairWidth);
        row.put("pixelAlignedNativeTitles", true);
        row.put("fontCache", inspectFontCache(titleStyle));
        row.put("twoLineNames", twoLines);
        row.put("fullNames", fullNames);
        row.put("displayedNameCharacters", characters);
        row.put("iconFlushes", graph.lastIconFlushCountForTest());
        if (SCENARIOS.stream().anyMatch(scenario -> scenario.name.equals(name))) {
            row.put("staticGeometryRebuilds", graph.renderPlanBuildCountForTest() - geometryBuilds);
            row.put("staticTitleRebuilds", graph.titlePlanBuildCountForTest() - titleBuilds);
        }
        observations.add(row);
        LOGGER.info("RESEARCH_UI_SCENARIO {}", row);
    }

    private static Map<String, Object> inspectFontCache(net.minecraft.network.chat.Style style) throws Exception {
        var resolve = net.minecraft.client.gui.Font.class.getDeclaredMethod("getFontSet", net.minecraft.resources.ResourceLocation.class);
        resolve.setAccessible(true);
        Object fontSet = resolve.invoke(Minecraft.getInstance().font, style.getFont());
        var textures = (List<?>) read(fontSet, "textures");
        long atlasBytes = 0;
        for (Object texture : textures) {
            int side = number(texture, "SIZE");
            atlasBytes += (long) side * side * ((Boolean) read(texture, "colored") ? 4 : 1);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("atlasCount", textures.size());
        result.put("atlasBaseLevelBytes", atlasBytes); // Pixel payload; not total driver VRAM.
        var glyphs = (net.minecraft.client.gui.font.CodepointMap<?>) read(fontSet, "glyphs");
        result.put("cachedGlyphs", glyphs.keySet().size());
        return result;
    }

    private static Object selectedNode() throws Exception {
        List<?> nodes = (List<?>) read(graph, "renderedNodeBuffer");
        for (int i = 0; i < number(graph, "renderedNodeCount"); i++) {
            if (selectedId.equals(read(read(nodes.get(i), "source"), "id"))) return nodes.get(i);
        }
        throw new IllegalStateException("Selected project must be visible");
    }

    private static Set<String> visibleResearchIds() throws Exception {
        Set<String> ids = new HashSet<>();
        List<?> nodes = (List<?>) read(graph, "renderedNodeBuffer");
        for (int i = 0; i < number(graph, "renderedNodeCount"); i++) {
            ids.add((String) read(read(nodes.get(i), "source"), "id"));
        }
        return ids;
    }

    private static void click(Minecraft mc) {
        mc.screen.mouseClicked(pointerX, pointerY, 0);
        mc.screen.mouseReleased(pointerX, pointerY, 0);
    }

    private static void centerSelected(double zoom) throws Exception {
        var position = ((ResearchGraphLayout) read(graph, "layout")).positions().get(selectedId);
        state.selectResearch(selectedId);
        graph.setCamera(new Camera(position.x() + 72, position.y() + 26, zoom));
    }

    private static void findEmptyPoint() throws Exception {
        List<?> nodes = (List<?>) read(graph, "renderedNodeBuffer");
        double nearest = Double.POSITIVE_INFINITY;
        for (int y = graphY() + 40; y < graphY() + graph.getHeight() - 16; y += 16) {
            for (int x = graphX() + 10; x < graphX() + graph.getWidth() - 10; x += 16) {
                double dx = x - graphX() - graph.getWidth() / 2d, dy = y - graphY() - graph.getHeight() / 2d;
                double distance = dx * dx + dy * dy;
                if (distance >= nearest) continue;
                boolean clear = true;
                for (int i = 0; i < number(graph, "renderedNodeCount"); i++) {
                    if (contains(nodes.get(i), x, y, 6)) { clear = false; break; }
                }
                if (clear) { pointerX = x; pointerY = y; nearest = distance; }
            }
        }
        require(Double.isFinite(nearest), "No blank-space point for the real input scenario");
    }

    private static boolean contains(Object bounds, int x, int y, int padding) throws Exception {
        return x >= number(bounds, "x") - padding && x <= number(bounds, "x") + number(bounds, "width") + padding
                && y >= number(bounds, "y") - padding && y <= number(bounds, "y") + number(bounds, "height") + padding;
    }

    private static void movePointer(double x, double y) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        double screenX = x * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
        double screenY = y * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight();
        GLFW.glfwSetCursorPos(window, screenX, screenY);
        // GLFW may suppress cursor callbacks while this automated client is unfocused.
        // Deliver the same Minecraft input callback; let normal screen/render traversal update CUI hover.
        if (mouseMove == null) {
            mouseMove = mc.mouseHandler.getClass().getDeclaredMethod("onMove", long.class, double.class, double.class);
            mouseMove.setAccessible(true);
        }
        mouseMove.invoke(mc.mouseHandler, window, screenX, screenY);
    }

    private static void screenshot(String name) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(OUTPUT.resolve(RUN).resolve(name + ".png"));
        }
    }

    private static <T> T child(UILayer parent, Class<T> type) {
        return parent.getElements().stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    // Read render outputs only here, instead of exposing mutable production buffers as test APIs.
    private static Object read(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int number(Object target, String name) throws Exception {
        return (Integer) read(target, name);
    }

    private static int graphX() throws Exception {
        return number(graph, "viewportScreenX");
    }

    private static int graphY() throws Exception {
        return number(graph, "viewportScreenY");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @SubscribeEvent
    public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        if (ENABLED && desk != null && !finished) {
            finish(new IllegalStateException("Client disconnected before research UI validation completed"));
        }
    }

    private static void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        Minecraft mc = Minecraft.getInstance();
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", failure == null ? "passed" : "failed");
            result.put("world", WORLD);
            result.put("screenshots", OUTPUT.resolve(RUN).toString());
            result.put("observations", observations);
            result.put("completedActions", action);
            result.put("intermediateZoomObserved", intermediateZoomObserved);
            result.put("zoomTransitionFrames", zoomFrames);
            result.put("stablePanFrames", stablePanFrames);
            result.put("comparedTextPixels", comparedTextPixels);
            result.put("staticObservationRestarts", staticObservationRestarts);
            result.put("repeatedPanTitleRebuilds", repeatedPanTitleRebuilds);
            result.put("farPanNewNodes", farPanNewNodes);
            result.put("farPanReturnTitleRebuilds", farPanReturnTitleRebuilds);
            result.put("sameLanguageReloadPassed", sameLanguageReloadPassed);
            result.put("resourceReloadTitleRebuilds", resourceReloadTitleRebuilds);
            result.put("failure", failure == null ? null : failure.toString());
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve("result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
            if (failure == null) LOGGER.info("RESEARCH_UI_PASSED scenarios={}, actions={}", observations.size(), action);
            else LOGGER.error("RESEARCH_UI_FAILED", failure);
        } catch (Exception outputFailure) {
            LOGGER.error("Could not write research UI validation result", outputFailure);
        } finally {
            if (panReferenceImage != null) {
                panReferenceImage.close();
                panReferenceImage = null;
            }
            if (originalLanguage != null) {
                mc.options.guiScale().set(originalScale);
                mc.options.framerateLimit().set(originalFrameLimit);
                mc.options.pauseOnLostFocus = originalPause;
                mc.options.languageCode = originalLanguage;
                mc.getLanguageManager().setSelected(originalLanguage);
                FHResearch.editor = originalEditor;
                mc.options.save();
            }
            mc.stop();
        }
    }
}
