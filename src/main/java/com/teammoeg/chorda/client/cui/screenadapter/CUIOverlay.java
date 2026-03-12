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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CUIOverlay implements IGuiOverlay, CUIScreen, IGlobalGuiHandler {
	public static final Set<CUIOverlay> CUI_OVERLAYS = new HashSet<>();

	@Getter
	PrimaryLayer primaryLayer;
	boolean isInited=false;
	@Getter
	protected boolean renderAboveScreen;
	protected Predicate<CUIOverlay> canInteract = o -> false;
	public static final Predicate<CUIOverlay> whenVisibleAndEnabled = o -> o.primaryLayer.isVisible() && o.primaryLayer.isEnabled();
	public static final Predicate<CUIOverlay> whenScreenOpened = o -> mc().screen != null && whenVisibleAndEnabled.test(o);
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
	public static final IGuiOverlay VIRTUAL_MOUSE_OVERLAY=(gui,graphics,pt,sw,sh)->{
		if (!isMouseCaptured) return;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, virtualMouseZ);//topmost?

		FGuis.drawRing(graphics, (int)mouseX, (int) mouseY, 3, 6, 0, 360,
			Colors.setAlpha(Colors.CYAN, 0xff));
		graphics.pose().popPose();
	};
	public static void startMouseCapture() {
		MouseCaptureUtil.startMouseCapture();
		isMouseCaptured=true;
		mouseX = ClientUtils.screenCenterX();
		mouseY = ClientUtils.screenCenterY();
	}
	public static void stopMouseCapture() {
		if(isMouseCaptured) {
			isMouseCaptured=false;
			MouseCaptureUtil.stopMouseCapture();
			mouseX = ClientUtils.screenCenterX();
			mouseY = ClientUtils.screenCenterY();
			
		}
	}

	public CUIOverlay(PrimaryLayer layer, boolean renderAboveScreen) {
		this.renderAboveScreen = renderAboveScreen;
		this.primaryLayer=layer;
		primaryLayer.setScreen(this);
		CUI_OVERLAYS.add(this);
	}
	public CUIOverlay(PrimaryLayer layer) {
		this(layer, false);
	}
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
	public void renderBackground(GuiGraphics graphics,int screenWidth, int screenHeight) {
		if (primaryLayer.shouldRenderGradient()) {
			graphics.fillGradient(0, 0, screenWidth, screenHeight, -1072689136, -804253680);
		}
	}
	@Override
	public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
		if(!isInited) {
			isInited=true;
			primaryLayer.initGui();
		}
		if (renderAboveScreen && mc().screen != null) return;
		renderOverlay(graphics, partialTick, screenWidth, screenHeight);
	}

	private void renderOverlay(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
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
		primaryLayer.updateGui(mouseX, mouseY, partialTick);
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
		builder.draw(graphics, (int)mouseX, (int)mouseY, primaryLayer.theme());
		graphics.pose().popPose();
		Cursor cs=primaryLayer.getCursor();
		if(cs==null)
			Cursor.reset();
		else
			cs.use();
	}

	public void tick() {
		primaryLayer.tick();
	}
	public void onScroll(MouseScrollingEvent scroll) {
		if(primaryLayer.onMouseScrolled(scroll.getScrollDelta()))
			scroll.setCanceled(true);
	}
	public void onKeyPress(InputEvent.Key key) {
		if(key.getAction()==InputConstants.PRESS) {
			if(primaryLayer.onKeyPressed(key.getKey(),key.getScanCode(), key.getModifiers()))
				key.setCanceled(true);
		}else if(key.getAction()==InputConstants.RELEASE)
			primaryLayer.onKeyRelease(key.getKey(),key.getScanCode(), key.getModifiers());
	}
	public void onMousePress(InputEvent.MouseButton.Pre mouse) {
		if(mouse.getAction()==InputConstants.PRESS) {
			if(primaryLayer.onMousePressed(MouseButton.of(mouse.getButton())))
				mouse.setCanceled(true);
		}else if(mouse.getAction()==InputConstants.RELEASE)
			primaryLayer.onMouseReleased(MouseButton.of(mouse.getButton()));
	
	}

	@SubscribeEvent
	public static void onScreenRender(ScreenEvent.Render.Post event) {
		for(NamedGuiOverlay overlay:GuiOverlayManager.getOverlays())
			if(overlay.overlay() instanceof CUIOverlay col && col.renderAboveScreen)
				col.renderOverlay(event.getGuiGraphics(), event.getPartialTick(), ClientUtils.screenWidth(), ClientUtils.screenHeight());
	}
	@SubscribeEvent
	public static void onOverlayScroll(MouseScrollingEvent scroll) {
		for(CUIOverlay col : CUI_OVERLAYS) {
			if(col.canInteract.test(col)) {
				col.onScroll(scroll);
			}
			if(scroll.isCanceled())break;
		}
	}
	@SubscribeEvent
	public static void onOverlayKeyPress(InputEvent.Key key) {
		for(CUIOverlay col : CUI_OVERLAYS) {
			if(col.canInteract.test(col)) {
				col.onKeyPress(key);
			}
			if(key.isCanceled())break;
		}
	}
	@SubscribeEvent
	public static void onOverlayMousePress(InputEvent.MouseButton.Pre mouse) {
		for(CUIOverlay col : CUI_OVERLAYS) {
			if(col.canInteract.test(col)) {
				col.onMousePress(mouse);
			}
			if(mouse.isCanceled())break;
		}
	
	}
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
