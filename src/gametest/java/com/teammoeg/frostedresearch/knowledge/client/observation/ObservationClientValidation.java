/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.FRMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Opt-in Minecraft input callbacks, rendering, menus and integrated-server round trips in a fresh disposable world. */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class ObservationClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedresearch.observationValidation");
    private static final Path OUTPUT = Path.of(System.getProperty("frostedresearch.observationValidationRoot", "."), "build/observation-ui-validation");
    private static final List<String> ROUTES = List.of("hand-key", "offhand-key", "hand-glass", "dropped-key", "chest-glass", "chest-shift-glass", "inventory-key", "inventory-glass", "inventory-shift-glass", "creative-key", "creative-glass");
    private static final List<String> passed = new ArrayList<>();
    private static final long START = System.nanoTime();
    private static boolean worldRequested, finished;
    private static volatile boolean prepared;
    private static volatile Throwable serverFailure;
    private static boolean optionsCaptured;
    public static boolean keyHeld, useHeld, shiftHeld;

    /** Feed the normal Minecraft callback; the validation-only mixin supplies the matching device hold state. */
    private static void key(int value, boolean down) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (value == GLFW.GLFW_KEY_N) keyHeld = down;
        if (value == GLFW.GLFW_KEY_LEFT_SHIFT) shiftHeld = down;
        var method = mc.keyboardHandler.getClass().getDeclaredMethod("keyPress", long.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(mc.keyboardHandler, mc.getWindow().getWindow(), value, 0, down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE,
                shiftHeld ? GLFW.GLFW_MOD_SHIFT : 0);
    }

    private static void use(boolean down) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        useHeld = down;
        var method = mc.mouseHandler.getClass().getDeclaredMethod("onPress", long.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(mc.mouseHandler, mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, shiftHeld ? GLFW.GLFW_MOD_SHIFT : 0);
    }
    private static int route, stage, ticks;
    private static boolean screenshot;
    private static AbstractContainerScreen<?> host;
    private static int originalScale, originalFps;
    private static boolean originalPause;
    private static InputConstants.Key originalObserveKey, originalUseKey;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            require(System.nanoTime() - START < 420_000_000_000L, "client validation timed out");
            if (serverFailure != null) throw new IllegalStateException("scene setup failed", serverFailure);
            if (!worldRequested && mc.level == null && mc.screen != null && mc.getOverlay() == null) {
                worldRequested = true;
                originalScale = mc.options.guiScale().get();
                originalFps = mc.options.framerateLimit().get();
                originalPause = mc.options.pauseOnLostFocus;
                originalObserveKey = ObservationClient.OBSERVE.getKey();
                originalUseKey = mc.options.keyUse.getKey();
                optionsCaptured = true;
                ObservationClient.OBSERVE.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_N));
                mc.options.keyUse.setKey(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
                KeyMapping.resetMapping();
                mc.options.pauseOnLostFocus = false;
                mc.options.framerateLimit().set(60);
                mc.options.guiScale().set(2);
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 800);
                mc.resizeDisplay();
                mc.createWorldOpenFlows().createFreshLevel("observation-validation-" + System.currentTimeMillis(),
                        new LevelSettings("Observation validation", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(755111011717574879L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.player == null || mc.getSingleplayerServer() == null || mc.getOverlay() != null) return;
            if (route == ROUTES.size()) { finish(null); return; }
            String name = ROUTES.get(route);
            ticks++;
            switch (stage) {
                case 0 -> {
                    if (mc.screen != null || ticks < 20) return;
                    prepared = false;
                    stage = 1;
                    ticks = 0;
                    mc.getSingleplayerServer().execute(() -> prepare(name));
                }
                case 1 -> {
                    if (!prepared || ticks < 15) return;
                    mc.player.setXRot(name.equals("dropped-key") || name.startsWith("chest") ? 0 : -90);
                    mc.player.setYRot(0);
                    GLFW.glfwFocusWindow(mc.getWindow().getWindow());
                    if (name.startsWith("inventory") || name.startsWith("creative")) {
                        mc.setScreen(new InventoryScreen(mc.player));
                        require(mc.screen instanceof AbstractContainerScreen<?>, "inventory opened");
                        host = (AbstractContainerScreen<?>) mc.screen;
                        if (name.startsWith("creative")) {
                            require(host instanceof CreativeModeInventoryScreen, "creative menu opened");
                            var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                            method.setAccessible(true);
                            method.invoke(host, BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INVENTORY));
                        }
                        if (name.endsWith("glass")) {
                            if (name.startsWith("creative")) host.getMenu().setCarried(FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
                            else mc.gameMode.handleInventoryMouseClick(0, 9, 0, ClickType.PICKUP, mc.player);
                        }
                        Slot paper = host.getMenu().slots.stream().filter(slot -> slot.container == mc.player.getInventory() && slot.getSlotIndex() == 10).findFirst().orElseThrow();
                        movePointer(host.getGuiLeft() + paper.x + 8, host.getGuiTop() + paper.y + 8);
                    }
                    stage = 2;
                    ticks = 0;
                }
                case 2 -> {
                    if (ticks < 5) return;
                    if (name.contains("shift")) key(GLFW.GLFW_KEY_LEFT_SHIFT, true);
                    if (name.endsWith("glass")) use(true);
                    else key(GLFW.GLFW_KEY_N, true);
                    stage = 3;
                    ticks = 0;
                }
                case 3 -> {
                    if (ticks == 5) {
                        var request = ObservationClient.class.getDeclaredField("request");
                        request.setAccessible(true);
                        FRMain.LOGGER.info("OBSERVATION_INPUT route={} focused={} key={} use={} main={} off={} request={} screen={}",
                                name, mc.isWindowActive(), GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_N),
                                GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT),
                                mc.player.getMainHandItem(), mc.player.getOffhandItem(), request.get(null), mc.screen);
                    }
                    require(ticks < 130, name + " did not produce a record; screen=" + mc.screen);
                    boolean inventory = name.startsWith("inventory") || name.startsWith("creative");
                    if (inventory ? !InventoryObservationOverlay.hasRecord() : !(mc.screen instanceof ObservationRecordScreen)) return;
                    if (inventory) {
                        require(mc.screen == host, "container remained open");
                        if (name.endsWith("glass")) require(host.getMenu().getCarried().is(FRContents.Items.MAGNIFYING_GLASS.get()), "cursor kept its magnifier");
                    }
                    release();
                    passed.add(name);
                    screenshot = true;
                    stage = 4;
                    ticks = 0;
                }
                case 4 -> {
                    if (ticks < 8 || screenshot) return;
                    key(GLFW.GLFW_KEY_ESCAPE, true);
                    key(GLFW.GLFW_KEY_ESCAPE, false);
                    stage = 5;
                    ticks = 0;
                }
                case 5 -> {
                    if (ticks < 8) return;
                    require(!InventoryObservationOverlay.hasRecord() && !(mc.screen instanceof ObservationRecordScreen), "discard closed the record");
                    if (host != null) require(mc.screen == host, "discard retained the container");
                    mc.setScreen(null);
                    host = null;
                    route++;
                    stage = ticks = 0;
                }
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void prepare(String name) {
        try {
            var server = Minecraft.getInstance().getSingleplayerServer();
            var level = server.overworld();
            var player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(name.startsWith("creative") ? GameType.CREATIVE : GameType.SURVIVAL);
            player.setInvulnerable(true);
            player.teleportTo(.5, 65, .5);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 4; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState());
                for (int y = 65; y <= 69; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10))) item.discard();
            player.getInventory().clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().selected = 0;
            player.getInventory().setItem(9, FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
            player.getInventory().setItem(10, new ItemStack(Items.PAPER, 4));
            if (name.equals("hand-key")) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
            if (name.equals("offhand-key")) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.PAPER));
            if (name.equals("hand-glass")) {
                player.setItemInHand(InteractionHand.MAIN_HAND, FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.PAPER));
            }
            if (name.equals("dropped-key")) {
                var item = new ItemEntity(level, .5, 66.5, 2.5, new ItemStack(Items.PAPER));
                item.setNoGravity(true);
                item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                item.setNeverPickUp();
                level.addFreshEntity(item);
            }
            if (name.startsWith("chest")) {
                player.setItemInHand(InteractionHand.MAIN_HAND, FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
                level.setBlockAndUpdate(new BlockPos(0, 66, 3), Blocks.CHEST.defaultBlockState());
            }
            player.inventoryMenu.broadcastChanges();
            prepared = true;
        } catch (Throwable failure) { serverFailure = failure; }
    }

    private static void movePointer(double x, double y) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        double sx = x * mc.getWindow().getGuiScale(), sy = y * mc.getWindow().getGuiScale();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), sx, sy);
        var move = mc.mouseHandler.getClass().getDeclaredMethod("onMove", long.class, double.class, double.class);
        move.setAccessible(true);
        move.invoke(mc.mouseHandler, mc.getWindow().getWindow(), sx, sy);
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!ENABLED || finished || !screenshot || event.phase != TickEvent.Phase.END) return;
        try {
            Files.createDirectories(OUTPUT);
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(OUTPUT.resolve(ROUTES.get(route) + ".png"));
            }
            screenshot = false;
        } catch (Throwable failure) { finish(failure); }
    }

    private static void release() {
        if (!optionsCaptured) return;
        try {
            key(GLFW.GLFW_KEY_N, false);
            key(GLFW.GLFW_KEY_LEFT_SHIFT, false);
            use(false);
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    private static void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        release();
        try {
            Files.createDirectories(OUTPUT);
            if (failure != null) {
                try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                    image.writeToFile(OUTPUT.resolve("failure.png"));
                }
            }
            Files.writeString(OUTPUT.resolve("result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                    "status", failure == null ? "passed" : "failed", "routes", passed, "failure", failure == null ? "" : failure.toString())));
        } catch (Exception outputFailure) { FRMain.LOGGER.error("Cannot write observation validation result", outputFailure); }
        if (failure != null) FRMain.LOGGER.error("Observation client validation failed", failure);
        Minecraft mc = Minecraft.getInstance();
        if (optionsCaptured) {
            mc.options.guiScale().set(originalScale);
            mc.options.framerateLimit().set(originalFps);
            mc.options.pauseOnLostFocus = originalPause;
            ObservationClient.OBSERVE.setKey(originalObserveKey);
            mc.options.keyUse.setKey(originalUseKey);
            KeyMapping.resetMapping();
        }
        mc.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
