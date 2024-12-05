package com.teammoeg.frostedheart.content.tips;

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.frostedheart.foundation.gui.DebugScreen;
import com.teammoeg.frostedheart.foundation.gui.widget.IconButton;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.tips.client.gui.EmptyScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.TipListScreen;
import com.teammoeg.frostedheart.content.tips.client.hud.TipHUD;
import com.teammoeg.frostedheart.util.client.RawMouseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TipRenderer {
    private static final Minecraft mc = Minecraft.getInstance();
    public static final List<TipElement> renderQueue = new ArrayList<>();
    public static TipHUD currentTip = null;

    @SubscribeEvent
    public static void renderOnHUD(RenderGuiEvent.Post event) {
        if (!FHConfig.CLIENT.renderTips.get())
            return;

        if (mc.player == null) {
            return;
        }

        if (renderQueue.isEmpty()) return;
        Screen current = mc.screen;
        if (current != null) {
            if (!(current instanceof ChatScreen) && !(current instanceof EmptyScreen) && !(current instanceof DebugScreen)) {

                return;
            }
        }

        if (currentTip == null) {
            currentTip = new TipHUD(renderQueue.get(0));
        }

        if (!currentTip.visible) {
            if (renderQueue.size() <= 1 && current instanceof EmptyScreen) {
                mc.popGuiLayer();
            }
            TipDisplayManager.removeCurrent();
            return;

            //TODO 键位绑定
        } else if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), 258) && current instanceof EmptyScreen) {
            mc.popGuiLayer();
        }

        currentTip.render(event.getGuiGraphics(), false);
    }

    @SubscribeEvent
    public static void renderOnGUI(ScreenEvent.Render.Post event) {
        if (!FHConfig.CLIENT.renderTips.get())
            return;

        Screen gui = event.getScreen();
        if (gui instanceof PauseScreen || gui instanceof ChatScreen || gui instanceof EmptyScreen) {
            int x = mc.getWindow().getGuiScaledWidth()-12;
            int y = mc.getWindow().getGuiScaledHeight()-26;
            if (IconButton.renderIconButton(event.getGuiGraphics(), IconButton.Icon.HISTORY, RawMouseHelper.getScaledX(), RawMouseHelper.getScaledY(), x, y, 0xFFFFFFFF, 0x80000000)) {
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
            currentTip = new TipHUD(renderQueue.get(0));
        }

        if (!currentTip.visible) {
            TipDisplayManager.removeCurrent();
            return;
        }

        currentTip.render(event.getGuiGraphics(), true);
    }
}
