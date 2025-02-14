package com.teammoeg.chorda.client;

public class MouseCaptureUtil {

	private MouseCaptureUtil() {

	}
	private static CapturableMouseHandler getHandler() {
		return (CapturableMouseHandler) ClientUtils.mc().mouseHandler;
	}
	public static boolean isMouseCaptured() {
		return getHandler().isMouseCaptured();
	}
	public static void setCaptureMouse(boolean captureMouse) {
		getHandler().setCaptureMouse(captureMouse);
	}
	public static double getAndResetCapturedX() {
		return getHandler().getAndResetCapturedX();
	}
	public static double getAndResetCapturedY() {
		return getHandler().getAndResetCapturedY();
	}
}
