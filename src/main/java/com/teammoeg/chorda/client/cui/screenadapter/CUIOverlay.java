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

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseCaptureUtil;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.FGuis;
import lombok.Getter;
import lombok.Setter;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * CUI覆盖层。在游戏HUD上渲染CUI界面，支持鼠标捕获、输入事件分发和虚拟鼠标指针。
 * 同时实现了JEI的IGlobalGuiHandler以提供排除区域。
 * <p>
 * CUI overlay. Renders CUI interfaces on the game HUD, supporting mouse capture, input event dispatching,
 * and a virtual mouse pointer. Also implements JEI's IGlobalGuiHandler to provide exclusion areas.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CUIOverlay implements IGuiOverlay, CUIScreen, IGlobalGuiHandler {
	/** 所有已注册的CUI覆盖层集合 / Set of all registered CUI overlays */
	public static final Set<CUIOverlay> CUI_OVERLAYS = new HashSet<>();

	@Getter
	PrimaryLayer primaryLayer;
	boolean isInited=false;
	@Getter
	protected boolean renderAboveScreen;
	protected Predicate<CUIOverlay> canInteract = whenAITakesOverTheWorld;
	public static final Predicate<CUIOverlay> whenAITakesOverTheWorld = o -> false;
	/** 当覆盖层可见且启用时允许交互 / Allows interaction when overlay is visible and enabled */
	public static final Predicate<CUIOverlay> whenVisibleAndEnabled = o -> o.primaryLayer.isVisible() && o.primaryLayer.isEnabled();
	/** 当有Screen打开且覆盖层可见启用时允许交互 / Allows interaction when a Screen is open and overlay is visible and enabled */
	public static final Predicate<CUIOverlay> whenScreenOpened = o -> mc().screen != null && whenVisibleAndEnabled.test(o);
	/** 当没有Screen打开且覆盖层可见启用时允许交互 / Allows interaction when no Screen is open and overlay is visible and enabled */
	public static final Predicate<CUIOverlay> whenScreenClosed = o -> mc().screen == null && whenVisibleAndEnabled.test(o);

	@Getter
	private static double mouseX = -1;
	@Getter
	private static double mouseY = -1;
	@Setter
	@Getter
	public static int virtualMouseZ=2000;
	@Getter
	private static boolean isMouseCaptured;
	/** 虚拟鼠标指针覆盖层，在鼠标被捕获时绘制一个环形光标 / Virtual mouse pointer overlay that draws a ring cursor when mouse is captured */
	public static final IGuiOverlay VIRTUAL_MOUSE_OVERLAY=(gui,graphics,pt,sw,sh)->{
		if (!isMouseCaptured) return;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, virtualMouseZ);//topmost?

		FGuis.drawRing(graphics, (int)mouseX, (int) mouseY, 3, 6, 0, 360,
			Colors.setAlpha(Colors.CYAN, 0xff));
		graphics.pose().popPose();
	};
	/**
	 * 开始鼠标捕获，将虚拟鼠标置于屏幕中心。
	 * <p>
	 * Starts mouse capture, positioning the virtual mouse at the screen center.
	 */
	public static void startMouseCapture() {
		MouseCaptureUtil.startMouseCapture();
		isMouseCaptured=true;
		mouseX = ClientUtils.screenCenterX();
		mouseY = ClientUtils.screenCenterY();
	}
	/**
	 * 停止鼠标捕获，恢复正常鼠标输入。
	 * <p>
	 * Stops mouse capture, restoring normal mouse input.
	 */
	public static void stopMouseCapture() {
		if(isMouseCaptured) {
			isMouseCaptured=false;
			MouseCaptureUtil.stopMouseCapture();
			mouseX = ClientUtils.screenCenterX();
			mouseY = ClientUtils.screenCenterY();
			
		}
	}

	/**
	 * 构造一个CUI覆盖层。
	 * <p>
	 * Constructs a CUI overlay.
	 *
	 * @param layer 主层 / the primary layer
	 * @param renderAboveScreen 是否在Screen之上渲染 / whether to render above the Screen
	 */
	public CUIOverlay(PrimaryLayer layer, boolean renderAboveScreen) {
		this.renderAboveScreen = renderAboveScreen;
		this.primaryLayer=layer;
		primaryLayer.setScreen(this);
		CUI_OVERLAYS.add(this);
	}

	/**
	 * 构造一个不在Screen之上渲染的CUI覆盖层。
	 * <p>
	 * Constructs a CUI overlay that does not render above the Screen.
	 *
	 * @param layer 主层 / the primary layer
	 */
	public CUIOverlay(PrimaryLayer layer) {
		this(layer, false);
	}

	/**
	 * 构造一个带自定义交互条件的CUI覆盖层。
	 * <p>
	 * Constructs a CUI overlay with a custom interaction predicate.
	 *
	 * @param layer 主层 / the primary layer
	 * @param renderAboveScreen 是否在Screen之上渲染 / whether to render above the Screen
	 * @param canInteract 交互条件谓词 / the interaction predicate
	 */
	public CUIOverlay(PrimaryLayer layer, boolean renderAboveScreen, Predicate<CUIOverlay> canInteract) {
		this(layer, renderAboveScreen);
		this.canInteract = canInteract;
	}

	public static Minecraft mc() {
		return ClientUtils.getMc();
	}
	@Override
	public Screen getScreen() {
		return null;
	}
	/**
	 * 渲染覆盖层的背景渐变。
	 * <p>
	 * Renders the overlay background gradient.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param screenWidth 屏幕宽度 / the screen width
	 * @param screenHeight 屏幕高度 / the screen height
	 */
	public void renderBackground(GuiGraphics graphics,int screenWidth, int screenHeight) {
		if (primaryLayer.shouldRenderGradient()) {
			graphics.fillGradient(0, 0, screenWidth, screenHeight, -1072689136, -804253680);
		}
	}
	@Override
	public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
		if (renderAboveScreen && mc().screen != null) return;
		renderOverlay(graphics, partialTick, screenWidth, screenHeight);
	}

	/**
	 * 执行覆盖层的完整渲染流程：背景、鼠标更新、UI绘制、工具提示和光标。
	 * <p>
	 * Performs the full overlay rendering pipeline: background, mouse update, UI drawing, tooltips, and cursor.
	 */
	private void renderOverlay(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
		if(!isInited) {
			isInited=true;
			primaryLayer.initGui();
		}
		primaryLayer.onBeforeRender();
		int x = primaryLayer.getX();
		int y = primaryLayer.getY();
		int w = primaryLayer.getWidth();
		int h = primaryLayer.getHeight();
		//backgound
		renderBackground(graphics, screenWidth, screenHeight);
		CGuiHelper.resetGuiDrawing();
		//update mouse
		if(isMouseCaptured) {
			mouseX += MouseCaptureUtil.getAndResetCapturedDeltaX();
			mouseY += MouseCaptureUtil.getAndResetCapturedDeltaY();
		} else {
			mouseX = MouseHelper.getScaledX();
			mouseY = MouseHelper.getScaledY();
		}
		//System.out.println("x="+x+"y="+y+"w="+w+"h="+h);
		primaryLayer.updateGui(x,y,mouseX, mouseY, partialTick);
		primaryLayer.updateMouseOver();
		RenderingHint hint=new RenderingHint();
		//ui background
		primaryLayer.render(graphics, x, y, w, h, hint);
		//ui foreground/overlay
		primaryLayer.drawForeground(graphics, x, y, w, h);
		//this.width = w;
		//this.height = h;
		TooltipBuilder builder=new TooltipBuilder(100);
		primaryLayer.getTooltip(builder);
		graphics.pose().pushPose();
		builder.draw(graphics, (int)mouseX, (int)mouseY, primaryLayer.theme());
		graphics.pose().popPose();
		Cursor cs=primaryLayer.getCursor();
		if(cs==null)
			Cursor.reset();
		else
			cs.use();
	}

	/**
	 * 执行覆盖层的逻辑更新。
	 * <p>
	 * Performs the overlay's logic tick.
	 */
	public void tick() {
		primaryLayer.tick();
	}

	/**
	 * 处理鼠标滚轮事件。
	 * <p>
	 * Handles mouse scroll events.
	 *
	 * @param scroll 滚轮事件 / the scroll event
	 */
	public void onScroll(MouseScrollingEvent scroll) {
		if(primaryLayer.onMouseScrolled(scroll.getScrollDelta()))
			scroll.setCanceled(true);
	}
	/**
	 * 处理键盘按键事件。
	 * <p>
	 * Handles keyboard key events.
	 *
	 * @param key 按键事件 / the key event
	 */
	public void onKeyPress(InputEvent.Key key) {
		if(key.getAction()==InputConstants.PRESS) {
			primaryLayer.onKeyPressed(key.getKey(),key.getScanCode(), key.getModifiers()); // InputEvent.Key is not cancelable
		} else if(key.getAction()==InputConstants.RELEASE)
			primaryLayer.onKeyRelease(key.getKey(),key.getScanCode(), key.getModifiers());
	}
	/**
	 * 处理鼠标按钮事件。
	 * <p>
	 * Handles mouse button events.
	 *
	 * @param mouse 鼠标按钮事件 / the mouse button event
	 */
	public void onMousePress(InputEvent.MouseButton.Pre mouse) {
		if(mouse.getAction()==InputConstants.PRESS) {
			if(primaryLayer.onMousePressed(MouseButton.of(mouse.getButton())))
				mouse.setCanceled(true);
		}else if(mouse.getAction()==InputConstants.RELEASE)
			primaryLayer.onMouseReleased(MouseButton.of(mouse.getButton()));
	
	}

	/**
	 * Screen渲染后事件处理器。用于渲染需要显示在Screen之上的覆盖层。
	 * <p>
	 * Post-screen-render event handler. Used to render overlays that need to display above the Screen.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onScreenRender(ScreenEvent.Render.Post event) {
		for(NamedGuiOverlay overlay:GuiOverlayManager.getOverlays())
			if(overlay.overlay() instanceof CUIOverlay col && col.renderAboveScreen)
				col.renderOverlay(event.getGuiGraphics(), event.getPartialTick(), ClientUtils.screenWidth(), ClientUtils.screenHeight());
	}
	/**
	 * 覆盖层滚轮事件分发。遍历所有可交互的覆盖层处理滚轮事件。
	 * <p>
	 * Overlay scroll event dispatcher. Iterates through all interactable overlays to handle scroll events.
	 */
	@SubscribeEvent
	public static void onOverlayScroll(MouseScrollingEvent scroll) {
		for(CUIOverlay col : CUI_OVERLAYS) {
			if(col.canInteract.test(col)) {
				col.onScroll(scroll);
			}
			if(scroll.isCanceled())break;
		}
	}
	/**
	 * 覆盖层键盘事件分发。遍历所有可交互的覆盖层处理键盘事件。
	 * <p>
	 * Overlay key event dispatcher. Iterates through all interactable overlays to handle key events.
	 */
	@SubscribeEvent
	public static void onOverlayKeyPress(InputEvent.Key key) {
		for(CUIOverlay col : CUI_OVERLAYS) {
			if(col.canInteract.test(col)) {
				col.onKeyPress(key);
			}
			if(key.isCanceled())break;
		}
	}
	/**
	 * 覆盖层鼠标按钮事件分发。遍历所有可交互的覆盖层处理鼠标按钮事件。
	 * <p>
	 * Overlay mouse button event dispatcher. Iterates through all interactable overlays to handle mouse button events.
	 */
	@SubscribeEvent
	public static void onOverlayMousePress(InputEvent.MouseButton.Pre mouse) {
		for(CUIOverlay col : CUI_OVERLAYS) {
			if(col.canInteract.test(col)) {
				col.onMousePress(mouse);
			}
			if(mouse.isCanceled())break;
		}
	
	}
	/**
	 * 客户端tick事件处理器。在每个tick开始时更新所有覆盖层，并在玩家不存在时停止鼠标捕获。
	 * <p>
	 * Client tick event handler. Updates all overlays at the start of each tick,
	 * and stops mouse capture when the player is absent.
	 */
	@SubscribeEvent
	public static void onTick(ClientTickEvent event) {
		if (event.phase == Phase.START) {
			if (ClientUtils.getPlayer() == null) {
				stopMouseCapture();
			}
			for(CUIOverlay col : CUI_OVERLAYS) {
				col.tick();
			}
		}
	}

	public static final Rect2i NONE = new Rect2i(0, 0, 0, 0);
	@Override
	public Collection<Rect2i> getGuiExtraAreas() {
		return List.of(renderAboveScreen && whenVisibleAndEnabled.test(this) ? primaryLayer.getBounds().toRect2i() : NONE);
	}
	//char typed event not available
	//mouse dragged event not available
//    @SubscribeEvent(priority=EventPriority.LOWEST)
//    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) { // 搬到 ClientEvents 里了，这里貌似注册不了
//    }
}
