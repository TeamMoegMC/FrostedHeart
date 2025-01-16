package com.teammoeg.chorda.widget;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 按下后显示另一条信息的 IconButton
 */
public class ActionStateIconButton extends IconButton {
    private final Component originalMessage;
    @Getter
    @Setter
    private Component clickedMessage;

    public ActionStateIconButton(int x, int y, Icon icon, int color, Component message, Component clickedMessage, OnPress pressedAction) {
        this(x, y, icon, color, 1, message, clickedMessage, pressedAction);
    }

    public ActionStateIconButton(int x, int y, Icon icon, int color, int scale, Component message, Component clickedMessage, OnPress pressedAction) {
        super(x, y, icon, color, scale, message, pressedAction);
        this.originalMessage = message;
        this.clickedMessage = clickedMessage;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        if (!isHovered()) {
            setMessage(originalMessage);
        }
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        super.onClick(pMouseX, pMouseY);
        setMessage(clickedMessage);
    }
}
