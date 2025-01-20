package com.teammoeg.frostedheart.content.tips.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class WheelMenuScreen extends Screen {
    private final Map<Button, Button.OnPress> actions = new HashMap<>();

    public WheelMenuScreen() {
        super(Component.literal("WheelSelector"));
        this.addRenderableWidget(Button.builder(Component.literal("test"), (p) -> {
            minecraft.player.sendSystemMessage(Component.literal("HELLO"));
        }).bounds(20, 20, 20, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void addSelection(Component message, Button.OnPress action, Object icon) {
        Button button = new Selection(message, action, icon);
        actions.put(button, action);
        this.addRenderableWidget(button);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == GLFW.GLFW_KEY_TAB) {
            return false;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    public static class Selection extends Button {
        IconType iconType;
        Object icon;

        public Selection(Component pMessage, OnPress pOnPress, Object icon) {
            super(0, 0, 0, 0, pMessage, pOnPress, Button.DEFAULT_NARRATION);
            this.iconType = getIconType(icon);
            this.icon = icon;
        }

        private IconType getIconType(Object obj) {
            IconType type;
            if (obj instanceof ItemStack) {
                type = IconType.ITEM;
            } else if (obj instanceof Renderable) {
                type = IconType.RENDERABLE;
            } else {
                type = IconType.EMPTY;
            }
            return type;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
            switch (iconType) {
                case ITEM -> graphics.renderItem((ItemStack) icon, getX(), getY());
                case ICON -> {
                    // TODO render icon
                }
                case RENDERABLE -> ((Renderable)icon).render(graphics, pMouseX, pMouseY, pPartialTick);
            }
        }

        protected Selection(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, CreateNarration pCreateNarration) {
            super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pCreateNarration);
        }

        public enum IconType {
            EMPTY,
            ITEM,
            ICON,
            RENDERABLE,
        }
    }
}
