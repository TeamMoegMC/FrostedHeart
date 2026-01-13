package com.teammoeg.chorda.client.cui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CUIMenuScreenWrapper<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements CUIScreen {
	private final PrimaryLayer primaryLayer;

	public CUIMenuScreenWrapper(PrimaryLayer g, T menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		primaryLayer = g;
		primaryLayer.setScreen(this);
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
        	boolean accepted=(primaryLayer.onMousePressed(MouseButton.of(button)));
        	System.out.println(accepted);
            return accepted || super.mouseClicked(x, y, button);
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

	@Override
	protected void renderBg(GuiGraphics graphics, float f, int mx, int my) {;
		CGuiHelper.resetGuiDrawing();
		primaryLayer.render(graphics, leftPos, topPos, imageWidth, imageHeight);
	}
	 List<Component> display=new ArrayList<>();
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		//graphics.pose().translate(-leftPos, -topPos, 0);
		CGuiHelper.resetGuiDrawing();

		primaryLayer.drawForeground(graphics, 0, 0, imageWidth, imageHeight);

		primaryLayer.getTooltip(display::add);

		 if (!display.isEmpty()){
			 graphics.pose().translate(0, 0, 600);
			 graphics.pose().translate(-leftPos, -topPos, 0);
	            graphics.setColor(1f, 1f, 1f, 0.8f);
	            graphics.renderTooltip(ClientUtils.getMc().font, display, Optional.empty(), mouseX, Math.max(mouseY, 18));
	            graphics.setColor(1f, 1f, 1f, 1f);
		}

		graphics.pose().popPose();
		display.clear();
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		if (primaryLayer.shouldRenderGradient()) {
			super.renderBackground(graphics);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
		Window win=super.minecraft.getWindow();
        leftPos=(win.getGuiScaledWidth() - primaryLayer.width) / 2;
        topPos=(win.getGuiScaledHeight() - primaryLayer.height) / 2;
		imageWidth = primaryLayer.width;
		imageHeight = primaryLayer.height;
		
		
		primaryLayer.updateGui(mouseX-leftPos, mouseY-topPos, partialTicks);
		primaryLayer.updateMouseOver();
		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void containerTick() {
		super.containerTick();
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

	@Override
	public Screen getScreen() {
		return this;
	}
}