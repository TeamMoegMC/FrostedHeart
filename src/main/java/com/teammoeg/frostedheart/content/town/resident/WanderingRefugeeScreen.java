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
        super(Component.translatable("screen.frostedheart.wandering_refugee.title"));
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