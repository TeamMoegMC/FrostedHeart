package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipWidget;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
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

    /**
     * TipWidget实例
     */
    public static final TipWidget TIP_WIDGET = new TipWidget();
    /**
     * tip渲染队列
     */
    public static final List<Tip> TIP_QUEUE = new ArrayList<>();
    /**
     * screen黑名单
     */
    public static final List<Class<? extends Screen>> SCREEN_BLACKLIST = new ArrayList<>();
    static {
        SCREEN_BLACKLIST.add(CommandBlockEditScreen.class);
    }

    /**
     * @return 是否正在渲染tip
     */
    public static boolean isTipRendering() {
        return TIP_WIDGET.getState() != TipWidget.State.IDLE;
    }

    /**
     * 移除当前显示的tip
     */
    public static void removeCurrent() {
        if (!TIP_QUEUE.isEmpty()) TIP_QUEUE.remove(0);
        TIP_WIDGET.setState(TipWidget.State.FADING_OUT);
    }

    @SubscribeEvent
    public static void onGuiInit(ScreenEvent.Init event) {
        if (!FHConfig.CLIENT.renderTips.get())
            return;
        if (SCREEN_BLACKLIST.contains(event.getScreen().getClass()))
            return;

        // 将TipWidget添加到当前screen中
        if (!event.getListenersList().contains(TIP_WIDGET)) {
            event.addListener(TIP_WIDGET.closeButton);
            event.addListener(TIP_WIDGET.pinButton);
            event.addListener(TIP_WIDGET);

            // 原版的物品和tooltip顺序可能在screen渲染之后,
            // 而我为了确保tip始终渲染在所有layout的最上层将
            // z轴偏移了+800,这导致了tip的半透明背景会因为渲
            // 染顺序而剔除这些元素
            //
            // 将tipWidget和按钮从screen的渲染列表中移除
            event.getScreen().renderables.remove(TIP_WIDGET.closeButton);
            event.getScreen().renderables.remove(TIP_WIDGET.pinButton);
            event.getScreen().renderables.remove(TIP_WIDGET);
        }
    }

    @SubscribeEvent
    public static void onHudRender(RenderGuiEvent.Post event) {
        if (!FHConfig.CLIENT.renderTips.get() || TIP_QUEUE.isEmpty())
            return;
        Minecraft MC = ClientUtils.mc();
        if (MC.screen != null && !SCREEN_BLACKLIST.contains(MC.screen.getClass()))
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
    public static void onGuiRender(ScreenEvent.Render.Post event) {
        if (!FHConfig.CLIENT.renderTips.get() || TIP_QUEUE.isEmpty())
            return;
        if (!event.getScreen().children().contains(TIP_WIDGET))
            return;

        // 避免点击tip后聊天栏无法编辑
        if (event.getScreen() instanceof ChatScreen) {
            for (GuiEventListener child : event.getScreen().children()) {
                if (child instanceof EditBox e) {
                    event.getScreen().setFocused(e);
                }
            }
        }

        TIP_WIDGET.renderWidget(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        update();
    }

    private static void update() {
        if (TIP_WIDGET.getState() == TipWidget.State.IDLE) {
            TIP_QUEUE.remove(TIP_WIDGET.lastTip);
            TIP_WIDGET.lastTip = null;
            // 切换下一个
            if (!TIP_QUEUE.isEmpty()) {
                TIP_WIDGET.setTip(TIP_QUEUE.get(0));
            }
        }
    }
}
