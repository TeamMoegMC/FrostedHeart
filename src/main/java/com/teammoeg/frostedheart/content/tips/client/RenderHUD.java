package com.teammoeg.frostedheart.content.tips.client;

import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.EmptyScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.TipListScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.hud.TipHUD;
import com.teammoeg.frostedheart.content.tips.client.util.GuiUtil;
import com.teammoeg.frostedheart.content.tips.client.util.TipDisplayUtil;
import com.teammoeg.frostedheart.content.tips.client.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderHUD {
    private static final Minecraft mc = Minecraft.getInstance();
    public static final List<TipElement> renderQueue = new ArrayList<>();
    public static TipHUD currentTip = null;

    @SubscribeEvent
    public static void renderOnHUD(RenderGuiEvent.Post event) {
        if ( mc.player == null) {
            return;
        }

        WaypointManager.renderWaypoints(event.getGuiGraphics());

        if (renderQueue.isEmpty()) return;
        Screen current = mc.screen;
        if (current != null) {
            if (!(current instanceof ChatScreen) && !(current instanceof EmptyScreen) && !(current instanceof DebugScreen)) {

                return;
            }
        }

        if (currentTip == null) {
            currentTip = new TipHUD(event.getGuiGraphics(), renderQueue.get(0));
        }

        if (!currentTip.visible) {
            if (renderQueue.size() <= 1 && current instanceof EmptyScreen) {
                mc.popGuiLayer();
            }
            TipDisplayUtil.removeCurrent();
            return;

        } else if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), 258) && current instanceof EmptyScreen) {
            mc.popGuiLayer();
        }

        currentTip.render(false);
    }

    @SubscribeEvent
    public static void renderOnGUI(ScreenEvent.Render.Post event) {
        Screen gui = event.getScreen();
        if (gui instanceof PauseScreen || gui instanceof ChatScreen || gui instanceof EmptyScreen) {
            int x = mc.getWindow().getGuiScaledWidth()-12;
            int y = mc.getWindow().getGuiScaledHeight()-26;
            if (GuiUtil.renderIconButton(event.getGuiGraphics(), IconButton.ICON_HISTORY, GuiUtil.getMouseX(), GuiUtil.getMouseY(), x, y, 0xFFFFFFFF, 0x80000000)) {
                mc.setScreen(new TipListScreen(gui instanceof PauseScreen));
            }
        }

        if (renderQueue.isEmpty() ||
                gui instanceof ChatScreen ||
                gui instanceof EmptyScreen ||
                gui instanceof TipListScreen ||
                gui instanceof DebugScreen) {

            return;
        }

        if (currentTip == null) {
            currentTip = new TipHUD(event.getGuiGraphics(), renderQueue.get(0));
        }

        if (!currentTip.visible) {
            TipDisplayUtil.removeCurrent();
            return;
        }

        currentTip.render(true);
    }
}
