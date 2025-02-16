package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.chorda.client.CapturableMouseHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin_MouseCapture implements CapturableMouseHandler {
	@Shadow
	private Minecraft minecraft;
	@Shadow
	private double xpos;
	@Shadow
	private double ypos;
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	private boolean captureMouse;
	private double capturedX;
	private double capturedY;
	@Override
	public boolean isMouseCaptured() {
		return captureMouse;
	}
	@Override
	public void setCaptureMouse(boolean captureMouse) {
		this.captureMouse = captureMouse;
		capturedX=0;
		capturedY=0;
	}

	
	@Override
	public double getAndResetCapturedX() {
		double cx=capturedX;
		capturedX-=cx;
		return cx;
	}
	@Override
	public double getAndResetCapturedY() {
		double cx=capturedY;
		capturedY-=cx;
		return cx;
	}
	public MouseHandlerMixin_MouseCapture() {

	}
	@Shadow
	public abstract boolean isMouseGrabbed();

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;turnPlayer()V"), method = "Lnet/minecraft/client/MouseHandler;onMove(JDD)V", require = 1)
	private void fh$captureMouse(long pWindowPointer, double pXpos, double pYpos, CallbackInfo cbi) {
		if(isMouseCaptured()) {
			if (this.isMouseGrabbed() && this.minecraft.isWindowActive()) {
				double dxpos = pXpos - xpos;
				double dypos = pYpos - ypos;
				this.accumulatedDX-=dxpos;
				this.accumulatedDY-=dypos;
				this.capturedX+=dxpos;
				this.capturedY+=dypos;
			}
		}
	}
}
