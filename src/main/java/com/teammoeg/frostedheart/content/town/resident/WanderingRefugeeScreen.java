/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.network.WanderingRefugeeOpenTradeGUIMessage;
import com.teammoeg.frostedheart.content.town.network.WanderingRefugeeRecruitMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class WanderingRefugeeScreen extends Screen {
    private final WanderingRefugee refugee;

    protected WanderingRefugeeScreen(WanderingRefugee refugee) {
        super(Component.translatable("entity.frostedheart.wandering_refugee"));
        this.refugee = refugee;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 150;
        int buttonHeight = 20;
        int centerX = (this.width - buttonWidth) / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.frostedheart.wandering_refugee.trade_button"),
                        (button) -> {
                            handleTradeAction();
                            this.onClose();
                        })
                .bounds(centerX, this.height / 2 - 30, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.frostedheart.wandering_refugee.recruit_button"),
                        (button) -> {
                            handleRecruitAction();
                            this.onClose();
                        })
                .bounds(centerX, this.height / 2, buttonWidth, buttonHeight)
                .build());
    }

    private void handleTradeAction() {
        FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeOpenTradeGUIMessage(refugee.getId()));
    }

    private void handleRecruitAction() {
        FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeRecruitMessage(refugee.getId()));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}