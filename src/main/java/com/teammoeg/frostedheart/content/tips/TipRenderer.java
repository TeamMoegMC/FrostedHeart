package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.EmptyScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.TipListScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.hud.TipHUD;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.RawMouseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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
    public static void renderOnHUD(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (renderQueue.isEmpty()) return;
        Screen current = mc.currentScreen;
        if (current != null) {
            if (!(current instanceof ChatScreen) && !(current instanceof EmptyScreen) && !(current instanceof DebugScreen)) {

                return;
            }
        }

        if (currentTip == null) {
            currentTip = new TipHUD(event.getMatrixStack(), renderQueue.get(0));
        }

        if (!currentTip.visible) {
            if (renderQueue.size() <= 1 && current instanceof EmptyScreen) {
                mc.popGuiLayer();
            }
            TipDisplayManager.removeCurrent();
            return;

        } else if (!InputMappings.isKeyDown(mc.getMainWindow().getHandle(), 258) && current instanceof EmptyScreen) {
            mc.popGuiLayer();
        }

        currentTip.render(false);
    }

    @SubscribeEvent
    public static void renderOnGUI(GuiScreenEvent.DrawScreenEvent.Post event) {
        Screen gui = event.getGui();
        if (gui instanceof IngameMenuScreen || gui instanceof ChatScreen || gui instanceof EmptyScreen) {
            int x = ClientUtils.screenWidth()-12;
            int y = ClientUtils.screenHeight()-26;
            if (FHGuiHelper.renderIconButton(event.getMatrixStack(), IconButton.ICON_HISTORY, RawMouseHelper.getScaledMouseX(), RawMouseHelper.getScaledMouseY(), x, y, 0xFFFFFFFF, 0x80000000)) {
                mc.displayGuiScreen(new TipListScreen(gui instanceof IngameMenuScreen));
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
            currentTip = new TipHUD(event.getMatrixStack(), renderQueue.get(0));
        }

        if (!currentTip.visible) {
            TipDisplayManager.removeCurrent();
            return;
        }

        currentTip.render(true);
    }
}
