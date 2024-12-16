package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipWidget;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TipRenderer {
    public static final List<Tip> renderQueue = new ArrayList<>();
    public static final TipWidget TIP_WIDGET = new TipWidget();
    // 不在GUI中渲染
    public static final List<Class<? extends Screen>> GUI_BLACKLIST = new ArrayList<>();
    static {
        GUI_BLACKLIST.add(CommandBlockEditScreen.class);
    }

    public static boolean isTipRendering() {
        return TIP_WIDGET.getState() != TipWidget.State.IDLE;
    }

    public static void removeCurrent() {
        if (!renderQueue.isEmpty()) renderQueue.remove(0);
        TIP_WIDGET.removeTip();
    }

    @SubscribeEvent
    public static void onGuiInit(ScreenEvent.Init.Pre event) {
        if (!FHConfig.CLIENT.renderTips.get())
            return;
        if (GUI_BLACKLIST.contains(event.getScreen().getClass()))
            return;

        // TODO 兼容JEI
        if (!event.getListenersList().contains(TIP_WIDGET)) {
            event.addListener(TIP_WIDGET.closeButton);
            event.addListener(TIP_WIDGET.pinButton);
            event.addListener(TIP_WIDGET);
            // 按钮由TipWidget渲染
            event.getScreen().renderables.remove(TIP_WIDGET.closeButton);
            event.getScreen().renderables.remove(TIP_WIDGET.pinButton);
        }
    }

    @SubscribeEvent
    public static void renderOnHUD(RenderGuiEvent.Post event) {
        if (!FHConfig.CLIENT.renderTips.get() || renderQueue.isEmpty())
            return;
        Minecraft MC = ClientUtils.mc();
        if (MC.screen != null && !GUI_BLACKLIST.contains(MC.screen.getClass()))
            return;

//        if (WIDGET_INSTANCE.getState() != TipWidget.State.IDLE) {
//            //TODO 键位绑定
//            if (InputConstants.isKeyDown(MC.getWindow().getWindow(), 258)) {
//                MC.screen = new WheelSelectorScreen();
//            } else if (MC.screen instanceof WheelSelectorScreen) {
//                MC.popGuiLayer();
//            }
//        } else {
//            if (MC.screen instanceof WheelSelectorScreen) {
//                MC.popGuiLayer();
//            }
//        }

        TIP_WIDGET.renderWidget(event.getGuiGraphics(), -1, -1, MC.getPartialTick());
        update();
    }

    @SubscribeEvent
    public static void onGuiRender(ScreenEvent.Render event) {
        if (!FHConfig.CLIENT.renderTips.get() || renderQueue.isEmpty() || !event.getScreen().children().contains(TIP_WIDGET))
            return;
        update();
    }

    private static void update() {
        if (TIP_WIDGET.getState() == TipWidget.State.IDLE) {
            // 删除当前
            renderQueue.remove(TIP_WIDGET.getTip());
            TIP_WIDGET.removeTip();
            // 切换下一个
            if (!renderQueue.isEmpty()) {
                TIP_WIDGET.setTip(renderQueue.get(0));
            }
        }
    }
}
