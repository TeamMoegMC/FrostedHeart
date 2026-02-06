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

package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.client.widget.ActionStateIconButton;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipEditsList;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TipEditorScreen extends Screen {
    private final TipEditsList list;

    public TipEditorScreen() {
        super(Components.str("Tip Editor"));
        this.list = new TipEditsList(ClientUtils.getMc(), ClientUtils.screenWidth() + 172, ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30, 28);
        list.setRenderBackground(false);
        list.setLeftPos(-172);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        list.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void resize(@NotNull Minecraft pMinecraft, int pWidth, int pHeight) {
        super.resize(pMinecraft, pWidth, pHeight);
        list.updateSize(ClientUtils.screenWidth() + 172, ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30);
        list.setLeftPos(-172);
    }

    @Override
    protected void init() {
        super.init();
        addWidget(list);
        // 保存按钮
        addRenderableWidget(new ActionStateIconButton(ClientUtils.screenWidth()/2 - 30, ClientUtils.screenHeight()-25, FlatIcon.FOLDER, Colors.themeColor(), 2, Component.translatable("gui.frostedheart.save_as_file"), Component.translatable("gui.frostedheart.saved"), (b) -> {
            var json = list.toJson();
            if (json != null) {
                Tip.builder("").fromJson(json).build().saveAsFile();
                TipManager.INSTANCE.loadFromFile();
            }
        }));
        // 发送按钮
        ActionStateIconButton sendButton = new ActionStateIconButton(ClientUtils.screenWidth()/2 + 5, ClientUtils.screenHeight()-25, FlatIcon.GIVE, Colors.themeColor(), 2, Component.translatable("gui.frostedheart.tip_editor.send"), Component.translatable("gui.frostedheart.sent"), (b) -> {
            var json = list.toJson();
            if (json != null) {
                Tip tip = Tip.builder("").fromJson(json).build();
                FHNetwork.INSTANCE.sendToServer(new DisplayCustomTipRequestPacket(tip));
            }
        });
        if (!ClientUtils.getPlayer().hasPermissions(2)) {
            sendButton.active = false;
            sendButton.setMessage(Component.translatable("gui.frostedheart.tip_editor.no_permission"));
        }
        addRenderableWidget(sendButton);
    }
}
