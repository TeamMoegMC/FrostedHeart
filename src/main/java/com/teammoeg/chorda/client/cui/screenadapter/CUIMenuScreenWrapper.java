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

import com.mojang.blaze3d.platform.Window;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;

/**
 * CUI菜单屏幕包装器。将PrimaryLayer适配到Minecraft的AbstractContainerScreen上，处理输入事件、渲染和生命周期管理。
 * <p>
 * CUI menu screen wrapper. Adapts a PrimaryLayer onto Minecraft's AbstractContainerScreen,
 * handling input events, rendering, and lifecycle management.
 *
 * @param <T> 容器菜单类型 / the container menu type
 */
public class CUIMenuScreenWrapper<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements CUIScreen {
	private final PrimaryLayer primaryLayer;

	/**
	 * 构造一个CUI菜单屏幕包装器。
	 * <p>
	 * Constructs a CUI menu screen wrapper.
	 *
	 * @param g 主层 / the primary layer
	 * @param menu 容器菜单 / the container menu
	 * @param playerInventory 玩家背包 / the player inventory
	 * @param title 屏幕标题 / the screen title
	 */
	public CUIMenuScreenWrapper(PrimaryLayer g, T menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		primaryLayer = g;
		primaryLayer.setScreen(this);
	}

	/** {@inheritDoc} */
	@Override
	public void init() {
		super.init();
		try {
			primaryLayer.initGui();
		}catch(Throwable t) {
			t.printStackTrace();
		}

	}

	/** {@inheritDoc} */
	@Override
	public boolean isPauseScreen() {
		return primaryLayer.isPauseScreen();
	}

	/**
	 * 处理鼠标点击事件，将事件转发给主层。鼠标侧键触发返回操作。
	 * <p>
	 * Handles mouse click events, forwarding them to the primary layer. Mouse side button triggers back navigation.
	 */
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        primaryLayer.updateGui(x-leftPos, y-topPos, -1);

        if (button == GLFW.GLFW_MOUSE_BUTTON_4) {
            primaryLayer.back();
            return true;
        } else {
        	boolean accepted=(primaryLayer.onMousePressed(MouseButton.of(button)));
        	
            return accepted || super.mouseClicked(x, y, button);
        }
    }


    @Override
    public boolean mouseReleased(double x, double y, int button) {
    	primaryLayer.updateGui(x-leftPos, y-topPos, -1);
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

	/**
	 * 渲染背景层，委托给主层进行绘制。
	 * <p>
	 * Renders the background layer, delegating drawing to the primary layer.
	 */
	@Override
	protected void renderBg(GuiGraphics graphics, float f, int mx, int my) {;
		CGuiHelper.resetGuiDrawing();
		primaryLayer.render(graphics, leftPos, topPos, imageWidth, imageHeight);
	}
	/**
	 * 渲染前景标签，包括工具提示和光标更新。
	 * <p>
	 * Renders foreground labels, including tooltips and cursor updates.
	 */
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		//graphics.pose().translate(-leftPos, -topPos, 0);
		CGuiHelper.resetGuiDrawing();
		
		primaryLayer.drawForeground(graphics, 0, 0, imageWidth, imageHeight);
		TooltipBuilder builder=new TooltipBuilder(600);
		primaryLayer.getTooltip(builder);

		graphics.pose().translate(-leftPos, -topPos, 0);
		builder.draw(graphics, mouseX, mouseY, primaryLayer.theme());
		graphics.pose().popPose();
		Cursor cs=primaryLayer.getCursor();
		if(cs==null)
			Cursor.reset();
		else
			cs.use();
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		if (primaryLayer.shouldRenderGradient()) {
			super.renderBackground(graphics);
		}
	}

	/**
	 * 主渲染方法。更新主层尺寸和位置，处理鼠标状态更新，然后调用父类渲染。
	 * <p>
	 * Main render method. Updates primary layer dimensions and position, handles mouse state updates,
	 * then calls the parent render.
	 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

		try {
			Window win=super.minecraft.getWindow();
			primaryLayer.onBeforeRender();
			imageWidth = primaryLayer.getWidth();
			imageHeight = primaryLayer.getHeight();
	        leftPos=(win.getGuiScaledWidth() - imageWidth) / 2;
	        topPos=(win.getGuiScaledHeight() - imageHeight) / 2;
	
			renderBackground(graphics);
			CGuiHelper.resetGuiDrawing();
			
			
			primaryLayer.updateGui(MouseHelper.getScaledX()-leftPos, MouseHelper.getScaledY()-topPos, partialTicks);
			primaryLayer.updateMouseOver();

		}catch(Throwable t) {
			t.printStackTrace();
		}
		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		primaryLayer.tick();
	}

	/**
	 * 屏幕被移除时调用，通知主层关闭并重置光标。
	 * <p>
	 * Called when the screen is removed, notifying the primary layer to close and resetting the cursor.
	 */
	@Override
	public void removed() {
		primaryLayer.onClosed();
		Cursor.reset();
		super.removed();
	}

	/** {@inheritDoc} */
	@Override
	public boolean shouldCloseOnEsc() {
		return primaryLayer.onCloseQuery();
	}

	/** {@inheritDoc} */
	@Override
	public PrimaryLayer getPrimaryLayer() {
		return primaryLayer;
	}

	/** {@inheritDoc} */
	@Override
	public Screen getScreen() {
		return this;
	}
}