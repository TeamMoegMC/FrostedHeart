package com.teammoeg.chorda.client.cui;

import org.lwjgl.glfw.GLFW;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.editor.EditDialog;
import com.teammoeg.chorda.client.cui.editor.EditorManager;

import lombok.Getter;
import lombok.Setter;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Primary layer for bridging between screen and layer infrastructure
 * */
public class PrimaryLayer extends UILayer implements LayerHolder,EditorManager {
	UIElement lastFocused;
	Screen prevScreen;
	@Getter
	@Setter
	private CUIScreen screen;
	public PrimaryLayer() {
		super(null);
		width = 176;
		height = 166;
		prevScreen = Minecraft.getInstance().screen;
		this.setScissorEnabled(false);
	}
	int mouseX;
	int mouseY;
	boolean hasBackGradient=true;
	boolean refreshRequested;

	public final void initGui() {
		if (onInit()) {
			this.refresh();
			finishInit();
		}
	}

	@Override
	public void focusOn(UIElement elm) {
		if(lastFocused==elm)return;
		if (lastFocused != null) {
			((Focusable) lastFocused).setFocused(false);
		}
		if (elm instanceof Focusable f) {
			f.setFocused(true);
			lastFocused = elm;
		}
	}

	@Override
	public Font getFont() {
		return ClientUtils.getMc().font;
	}



	@Override
	public LayerHolder getLayerHolder() {
		return this;
	}
	@Override
	public boolean shouldRenderGradient() {
		return hasBackGradient;
	}
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
			GLFW.glfwSetCursorPos(ClientUtils.getMc().getWindow().getWindow(), mx, my);
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
	public final void refreshElements() {
		refreshRequested = true;
	}

	@Override
	public final void updateGui(double mx, double my, float pt) {

		super.updateRenderInfo(mx, my, pt);


	}
	public void onBeforeRender() {
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
		//CGuiHelper.drawUIBackground(graphics, x, y, w, h);
	}

	public void drawForeground(GuiGraphics graphics, int x, int y, int w, int h) {
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if(super.onMousePressed(button)) {
			return true;
		}else this.focusOn(null);
		return false;
	}

	@Override
	public int getX() {
		return 0;
	}

	@Override
	public int getY() {
		return 0;
	}

	@Override
	public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		
		if (lastFocused != null && lastFocused.onKeyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (super.onKeyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if(keyCode==GLFW.GLFW_KEY_F12) {
			CUIDebugHelper.toggleDebug();
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
		if(dialog!=null)
			add(dialog);
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
		
		super.refresh();
		setSizeToContentSize();
		
	}

	public void setSizeToContentSize() {
		this.width=this.getContentWidth();
		this.height=this.getContentHeight();
	}
	public boolean onInit() {
		return true;
	}

	void finishInit() {
	}
	EditDialog dialog;
    public void closeDialog(boolean quit) {
        this.dialog = null;
        if (quit) {
            closeGui();
        }
        
    }

    public EditDialog getDialog() {
        return dialog;
    }
    public void openDialog(EditDialog dialog, boolean refresh) {
        this.dialog = dialog;
        if (refresh)
            this.refreshElements();
    }

	@Override
	public void closeGui() {
		closeGui(true);
		
	}

	@Override
	public CUIScreen getManager() {
		return screen;
	}


}
