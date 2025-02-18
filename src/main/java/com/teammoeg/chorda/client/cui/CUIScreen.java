package com.teammoeg.chorda.client.cui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CUIScreen extends Screen implements CUIScreenManager {
    private final PrimaryLayer primaryLayer;

    public CUIScreen(PrimaryLayer g) {
        super(g.getTitle());
        primaryLayer = g;
    }

    @Override
    public void init() {
        super.init();
        primaryLayer.refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return primaryLayer.isPauseScreen();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
    	primaryLayer.updateGui(x, y, -1);

        if (button == GLFW.GLFW_MOUSE_BUTTON_4) {
            primaryLayer.back();
            return true;
        } else {
        	
            return (primaryLayer.onMousePressed(MouseButton.of(button))) || super.mouseClicked(x, y, button);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
    	primaryLayer.updateGui(x, y, -1);
        primaryLayer.onMouseReleased(MouseButton.of(button));
        return super.mouseReleased(x, y, button);
    }



	@Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return primaryLayer.onMouseScrolled(delta) || super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        return primaryLayer.onMouseDragged(MouseButton.of(button), dragX, dragY) || super.mouseDragged(x, y, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (primaryLayer.onKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else {
            if (CInputHelper.isBackspace(keyCode)) {
                primaryLayer.back();
                return true;
            } else if (CInputHelper.shouldCloseMenu(keyCode, scanCode)) {
                if (shouldCloseOnEsc()) {
                    primaryLayer.closeGui(true);
                }
                return true;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        primaryLayer.onKeyRelease(keyCode, scanCode, modifiers);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char keyChar, int modifiers) {
        if (primaryLayer.onIMEInput(keyChar, modifiers)) {
            return true;
        }

        return super.charTyped(keyChar, keyChar);
    }
    List<Component> display=new ArrayList<>();
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        primaryLayer.updateGui(mouseX, mouseY, partialTicks);
        renderBackground(graphics);
        CGuiHelper.resetGuiDrawing();
        var x = primaryLayer.getX();
        var y = primaryLayer.getY();
        var w = primaryLayer.width;
        var h = primaryLayer.height;
        primaryLayer.render(graphics, x, y, w, h);
        primaryLayer.drawForeground(graphics, x, y, w, h);

        
        primaryLayer.getTooltip(display::add);
        graphics.pose().pushPose();
        if (!display.isEmpty()){
            graphics.pose().translate(0, 0, 100);
            graphics.setColor(1f, 1f, 1f, 0.8f);
            graphics.renderTooltip(ClientUtils.mc().font, display, Optional.empty(), mouseX, Math.max(mouseY, 18));
            graphics.setColor(1f, 1f, 1f, 1f);
        }
        graphics.pose().popPose();
        display.clear();
    }

    @Override
    public void renderBackground(GuiGraphics matrixStack) {
        if (primaryLayer.shouldRenderGradient()) {
            super.renderBackground(matrixStack);
        }
    }

    @Override
    public void tick() {
        super.tick();
        primaryLayer.tick();
    }

    @Override
    public void removed() {
        primaryLayer.onClosed();
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return primaryLayer.onCloseQuery();
    }

	@Override
	public PrimaryLayer getPrimaryLayer() {
		return primaryLayer;
	}
}