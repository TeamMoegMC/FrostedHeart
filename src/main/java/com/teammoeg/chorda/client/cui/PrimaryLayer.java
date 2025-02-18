package com.teammoeg.chorda.client.cui;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PrimaryLayer extends Layer implements LayerHolder {
	Focusable lastFocused;
	Screen prevScreen;

	public PrimaryLayer() {
		super(null);
		width = 176;
		height = 166;
		prevScreen = Minecraft.getInstance().screen;

	}

	int mouseX;
	int mouseY;
	boolean hasBackGradient;
	boolean refreshRequested;

	public final void initGui() {

		if (onInit()) {
			this.refresh();
			alignWidgets();
			finishInit();
		}
	}

	@Override
	public void focusOn(Focusable elm) {
		if (lastFocused != null) {
			lastFocused.setFocused(false);
		}
		if (elm != null) {
			elm.setFocused(true);
			lastFocused = elm;
		}
	}

	@Override
	public Font getFont() {
		return ClientUtils.mc().font;
	}

	@Override
	public int getErrorColor() {
		return 0xa92b0d;
	}

	@Override
	public int getHighlightColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public int getFrameColor() {
		return -6250336;
	}

	@Override
	public int getFontColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public int getButtonShadowColor() {
		return -8355712;
	}

	@Override
	public int getBackgroundColor() {
		return -16777216;
	}

	@Override
	public int getButtonFaceColor() {
		return -4144960;
	}

	@Override
	public LayerHolder getLayerHolder() {
		return this;
	}

	/**
	 * @return if the GUI should render a blur effect behind it
	 */
	@Override
	public boolean shouldRenderGradient() {
		return hasBackGradient;
	}

	/**
	 * @param renderBlur sets if the GUI should render a blur effect behind it
	 */
	public void setRenderGradient(boolean renderGradient) {
		this.hasBackGradient = renderGradient;
	}

	@Override
	public boolean onCloseQuery() {
		return true;
	}

	@Override
	public Screen getPrevScreen() {
		if (prevScreen instanceof ChatScreen) {
			return null;
		}

		return prevScreen;
	}

	@Override
	public final void closeGui(boolean openPrevScreen) {
		var mx = Minecraft.getInstance().mouseHandler.xpos();
		var my = Minecraft.getInstance().mouseHandler.ypos();

		var mc = Minecraft.getInstance();

		if (mc.player != null) {
			mc.player.closeContainer();

			if (mc.screen == null) {
				mc.setWindowActive(true);
			}
		}

		if (openPrevScreen && getPrevScreen() != null) {
			mc.setScreen(getPrevScreen());
			GLFW.glfwSetCursorPos(ClientUtils.mc().getWindow().getWindow(), mx, my);
		}

		onClosed();
	}

	public void back() {
		closeGui(true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public final void refreshWidgets() {
		refreshRequested = true;
	}

	@Override
	public final void updateGui(double mx, double my, float pt) {

		super.updateRenderInfo(mouseX, mouseY, pt);
		if (refreshRequested) {
			refresh();
			refreshRequested = false;
		}

	}

	@Override
	public final void render(GuiGraphics graphics, int x, int y, int w, int h) {
		super.render(graphics, x, y, w, h);
	}

	@Override
	public void onClosed() {
		super.onClosed();
		Cursor.reset();
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		CGuiHelper.drawUIBackground(graphics, x, y, w, h);
	}

	public void drawForeground(GuiGraphics graphics, int x, int y, int w, int h) {
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		return super.onMousePressed(button);
	}

	@Override
	public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (lastFocused != null && lastFocused.onKeyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (super.onKeyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean onMouseScrolled(double scroll) {
		if (lastFocused != null && lastFocused.onMouseScrolled(scroll)) {
			return true;
		}
		return super.onMouseScrolled(scroll);
	}

	@Override
	public boolean onIMEInput(char c, int modifiers) {
		if (lastFocused != null && lastFocused.onIMEInput(c, modifiers)) {
			return true;
		}
		return super.onIMEInput(c, modifiers);
	}

	public boolean isMouseOver(UIElement widget) {
		return MouseHelper.isMouseIn(mouseX, mouseY, widget.getScreenX(), widget.getScreenY(), width, height);

	}

	@Override
	public void addUIElements() {

	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public int getScreenX() {
		return getX();
	}

	@Override
	public int getScreenY() {
		return getY();
	}

	@Override
	public void refresh() {

		Window win = ClientUtils.mc().getWindow();
		this.setPos((win.getGuiScaledWidth() - width) / 2, (win.getGuiScaledWidth() - width) / 2);
		super.refresh();
	}

	public boolean onInit() {
		return true;
	}

	void finishInit() {
	}

}
