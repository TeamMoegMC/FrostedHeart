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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipWidget;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuRenderer;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TipRenderer {

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
        return TipWidget.INSTANCE.getState() != TipWidget.State.IDLE;
    }

    /**
     * 移除当前显示的tip
     */
    public static void removeCurrent() {
        TipWidget.INSTANCE.close();
    }

    public static void forceClose() {
        TipWidget.INSTANCE.resetState();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiInit(ScreenEvent.Init.Post event) {
        if (!FHConfig.CLIENT.enableTip.get())
            return;
        if (SCREEN_BLACKLIST.contains(event.getScreen().getClass()))
            return;

        // 将TipWidget添加到当前screen中
        if (!event.getListenersList().contains(TipWidget.INSTANCE)) {
            event.addListener(TipWidget.INSTANCE.closeButton);
            event.addListener(TipWidget.INSTANCE.pinButton);
            event.addListener(TipWidget.INSTANCE.linkButton);
            event.addListener(TipWidget.INSTANCE);

            // 将tipWidget和按钮从screen的渲染列表中移除
            event.getScreen().renderables.remove(TipWidget.INSTANCE.closeButton);
            event.getScreen().renderables.remove(TipWidget.INSTANCE.pinButton);
            event.getScreen().renderables.remove(TipWidget.INSTANCE.linkButton);
            event.getScreen().renderables.remove(TipWidget.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void onHudRender(RenderGuiEvent.Post event) {
        if (!FHConfig.CLIENT.enableTip.get() || (TIP_QUEUE.isEmpty() && !isTipRendering()))
            return;
        Minecraft MC = ClientUtils.getMc();
        if (MC.screen != null && !SCREEN_BLACKLIST.contains(MC.screen.getClass()))
            return;

        if (WheelMenuRenderer.isOpened()) {
            var pos = WheelMenuRenderer.getMousePos();
            int x = pos.getX()/2 + ClientUtils.screenWidth()/2;
            int y = pos.getY()/2 + ClientUtils.screenHeight()/2;
            TipWidget.INSTANCE.renderWidget(event.getGuiGraphics(), x, y, MC.getPartialTick());
            if (MouseHelper.isLeftClicked()) {
                if (TipWidget.INSTANCE.closeButton.isHoveredOrFocused()) {
                    TipWidget.INSTANCE.closeButton.onClick(x, y);
                } else if (TipWidget.INSTANCE.linkButton.isHoveredOrFocused()) {
                    TipWidget.INSTANCE.linkButton.onClick(x, y);
                } else if (TipWidget.INSTANCE.pinButton.isHoveredOrFocused()) {
                    TipWidget.INSTANCE.pinButton.onClick(x, y);
                }
            }
        } else {
            TipWidget.INSTANCE.renderWidget(event.getGuiGraphics(), -1, -1, MC.getPartialTick());
        }
        update();
    }

    @SubscribeEvent
    public static void onGuiRender(ScreenEvent.Render.Post event) {
        if (FrostedHud.renderDebugOverlay) {
            FrostedHud.renderDebugOverlay(event.getGuiGraphics(), ClientUtils.getMc());
        }
        if (!FHConfig.CLIENT.enableTip.get() || (TIP_QUEUE.isEmpty() && !isTipRendering()))
            return;
        if (!event.getScreen().children().contains(TipWidget.INSTANCE))
            return;

        // 避免点击tip后聊天栏无法编辑
        if (event.getScreen() instanceof ChatScreen) {
            for (GuiEventListener child : event.getScreen().children()) {
                if (child instanceof EditBox e) {
                    event.getScreen().setFocused(e);
                }
            }
        }

        TipWidget.INSTANCE.renderWidget(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        update();
    }

    private static void update() {
        if (!isTipRendering()) {
            TIP_QUEUE.remove(TipWidget.INSTANCE.lastTip);
            TipWidget.INSTANCE.lastTip = null;
            // 切换下一个
            if (!TIP_QUEUE.isEmpty()) {
                TipWidget.INSTANCE.tip = TIP_QUEUE.get(0);
            }
        }
    }
}
