/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.item.MagnifyingGlassItem;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;

/** Renders above the live container; replacing its Screen would close the menu and return the carried tool. */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class InventoryObservationOverlay {
    private static AbstractContainerScreen<?> host;
    private static ObservationRecordScreen record;
    private static int elapsed, duration;
    private static AbstractContainerScreen<?> rightCaptureHost;

    private InventoryObservationOverlay() {
    }

    static void begin(AbstractContainerScreen<?> screen) {
        host = screen;
        elapsed = duration = 0;
    }

    static boolean isHost(AbstractContainerScreen<?> screen) {
        return host == screen;
    }

    static boolean hasRecord() {
        return record != null;
    }

    static void started(int ticks) {
        duration = ticks;
        elapsed = 0;
    }

    static void record(Observation observation) {
        record = new ObservationRecordScreen(observation, InventoryObservationOverlay::clear);
        Minecraft mc = Minecraft.getInstance();
        record.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    static void rejected(String reason) {
        if (record != null) record.rejected(reason);
    }

    static void clear() {
        host = null;
        record = null;
        elapsed = duration = 0;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive() || mc.screen != rightCaptureHost) rightCaptureHost = null;
        if (host == null) return;
        if (mc.screen != host) {
            if (record != null)
                FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.DISCARD));
            ObservationClient.cancelHeld();
            clear();
        } else if (record != null) record.tick();
        else if (duration > 0) elapsed++;
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (event.getScreen() != host) return;
        var graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        if (record != null) {
            if (record.width != host.width || record.height != host.height)
                record.resize(Minecraft.getInstance(), host.width, host.height);
            graphics.fill(0, 0, host.width, host.height, 0x80000000);
            record.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        } else if (duration > 0) {
            int x = host.width / 2, y = host.height - 34;
            float progress = Math.min(1, (elapsed + event.getPartialTick()) / duration);
            graphics.fill(x - 110, y - 5, x + 110, y + 25, 0xE0302A21);
            graphics.drawCenteredString(Minecraft.getInstance().font, t("observation.hold", ObservationClient.inputHint()), x, y, 0xFFD8C69E);
            graphics.fill(x - 64, y + 16, x + 64, y + 18, 0xFF625B4C);
            graphics.fill(x - 64, y + 16, x - 64 + Math.round(128 * progress), y + 18, 0xFFD8C69E);
        }
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && event.getScreen() instanceof AbstractContainerScreen<?> screen
                && screen.getMenu().getCarried().getItem() instanceof MagnifyingGlassItem) {
            Slot slot = screen.getSlotUnderMouse();
            if (slot != null && slot.isActive() && ItemRetrieval.canObserve(slot.getItem())) {
                // A cancelled press must not leak its release/drag into vanilla slot placement.
                rightCaptureHost = screen;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getScreen() == rightCaptureHost) {
            rightCaptureHost = null;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY());
            event.setCanceled(true);
        } else if (event.getScreen() == rightCaptureHost && event.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            event.setCanceled(true);
        } else if (event.getScreen() instanceof AbstractContainerScreen<?>
                && ObservationClient.OBSERVE.matches(event.getKeyCode(), event.getScanCode())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.keyReleased(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void typed(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() == host && record != null) {
            record.charTyped(event.getCodePoint(), event.getModifiers());
            event.setCanceled(true);
        }
    }
}
