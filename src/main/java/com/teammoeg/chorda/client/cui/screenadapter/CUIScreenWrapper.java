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

package com.teammoeg.chorda.client.cui.screenadapter;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.Components;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class CUIScreenWrapper extends Screen implements CUIScreen {
	@Getter
	private final PrimaryLayer primaryLayer;


	public CUIScreenWrapper(PrimaryLayer layer) {
		super(Components.str(""));
		primaryLayer=layer;
		primaryLayer.setScreen(this);
	}

	@Override
	public void init() {
		super.init();
		try {
			primaryLayer.initGui();
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return primaryLayer.isPauseScreen();
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		primaryLayer.updateGui(x-this.x, y-this.y, -1);

		if (button == GLFW.GLFW_MOUSE_BUTTON_4) {
			primaryLayer.back();
			return true;
		} else {

			return (primaryLayer.onMousePressed(MouseButton.of(button))) || super.mouseClicked(x, y, button);
		}
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		primaryLayer.updateGui(x-this.x, y-this.y, -1);
		primaryLayer.onMouseReleased(MouseButton.of(button));
		return super.mouseReleased(x, y, button);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double delta) {
		return primaryLayer.onMouseScrolled(delta) || super.mouseScrolled(x-this.x, y-this.y, delta);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
		return primaryLayer.onMouseDragged(MouseButton.of(button), dragX-this.x, dragY-this.y) || super.mouseDragged(x, y, button, dragX, dragY);
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

	int x,y;
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

		
		Window win = super.minecraft.getWindow();
		primaryLayer.onBeforeRender();
		int w = primaryLayer.getWidth();
		int h = primaryLayer.getHeight();
		x = (win.getGuiScaledWidth() - w) / 2;
		y = (win.getGuiScaledHeight() - h) / 2;
		//backgound
		renderBackground(graphics);
		CGuiHelper.resetGuiDrawing();
		//update mouse
		//System.out.println("x="+x+"y="+y+"w="+w+"h="+h)

		primaryLayer.updateGui(MouseHelper.getScaledX() - x, MouseHelper.getScaledY() - y, partialTicks);
		primaryLayer.updateMouseOver();
		//ui background
		primaryLayer.render(graphics, x, y, w, h);
		//ui foreground/overlay
		primaryLayer.drawForeground(graphics, x, y, w, h);
		//this.width = w;
		//this.height = h;
		TooltipBuilder builder=new TooltipBuilder(100);
		primaryLayer.getTooltip(builder);
		graphics.pose().pushPose();
		builder.draw(graphics, mouseX, mouseY);
		graphics.pose().popPose();
		Cursor cs=primaryLayer.getCursor();
		if(cs==null)
			Cursor.reset();
		else
			cs.use();
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
		Cursor.reset();
		super.removed();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return primaryLayer.onCloseQuery();
	}
	@Override
	public Screen getScreen() {
		return this;
	}
}