package com.teammoeg.chorda.client.cui;

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseCaptureUtil;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.FGuis;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CUIOverlay implements IGuiOverlay, CUIScreen {
	@Getter
	PrimaryLayer primaryLayer;
	boolean isInited=false;
	@Getter
	private static double mouseX;
	@Getter
	private static double mouseY;
	public static final IGuiOverlay VIRTUAL_MOUSE_OVERLAY=(gui,graphics,pt,sw,sh)->{
		FGuis.drawRing(graphics, (int)mouseX, (int) mouseY, 3, 6, 0, 360,
			Colors.setAlpha(Colors.CYAN, 0xff));
	};
	@Getter
	private static boolean isMouseCaptured;
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
	public CUIOverlay(PrimaryLayer layer) {
		this.primaryLayer=layer;
		primaryLayer.setScreen(this);
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
		primaryLayer.onBeforeRender();
		int x = primaryLayer.getX();
		int y = primaryLayer.getY();
		int w = primaryLayer.width;
		int h = primaryLayer.height;
		//backgound
		renderBackground(graphics, screenWidth, screenHeight);
		CGuiHelper.resetGuiDrawing();
		//update mouse
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
		builder.draw(graphics, (int)mouseX, (int)mouseY);
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
	public static void onOverlayScroll(MouseScrollingEvent scroll) {
		for(NamedGuiOverlay overlay:GuiOverlayManager.getOverlays()) {
			if(overlay.overlay() instanceof CUIOverlay col) {
				col.onScroll(scroll);
			}
			if(scroll.isCanceled())break;
		}
	}
	@SubscribeEvent
	public static void onOverlayKeyPress(InputEvent.Key key) {
		for(NamedGuiOverlay overlay:GuiOverlayManager.getOverlays()) {
			if(overlay.overlay() instanceof CUIOverlay col) {
				col.onKeyPress(key);
			}
			if(key.isCanceled())break;
		}
	}
	@SubscribeEvent
	public static void onOverlayMousePress(InputEvent.MouseButton.Pre mouse) {
		for(NamedGuiOverlay overlay:GuiOverlayManager.getOverlays()) {
			if(overlay.overlay() instanceof CUIOverlay col) {
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
			if(isMouseCaptured) {
				mouseX += MouseCaptureUtil.getAndResetCapturedDeltaX();
				mouseY += MouseCaptureUtil.getAndResetCapturedDeltaY();
			}
			for(NamedGuiOverlay overlay:GuiOverlayManager.getOverlays()) {
				if(overlay.overlay() instanceof CUIOverlay col) {
					col.tick();
				}
			}
		}
	}
	//char typed event not available
	//mouse dragged event not available

}
