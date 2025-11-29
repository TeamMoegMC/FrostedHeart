package com.teammoeg.chorda.client;

/**
 * A convenience utility class for handling mouse capture without screen
 * This class is generally threadsafe
 */
public class MouseCaptureUtil {

	private MouseCaptureUtil() {

	}
	private static CapturableMouseHandler getHandler() {
		return (CapturableMouseHandler) ClientUtils.getMc().mouseHandler;
	}
	
	/**
	 * Checks if mouse captured.
	 *
	 * @return true, if mouse is captured
	 */
	public static boolean isMouseCaptured() {
		return getHandler().isMouseCaptured();
	}
	
	/**
	 * Start mouse capture, meaning mouse movement no longer move player's camera
	 */
	public static void startMouseCapture() {
		getHandler().setCaptureMouse(true);
	}
	
	/**
	 * Stop mouse capture.
	 */
	public static void stopMouseCapture() {
		getHandler().setCaptureMouse(false);
	}
	
	/**
	 * Gets the and reset captured delta X
	 * Get the delta x movement between current and previous position, then set the x delta movement back to 0 for next capture
	 * @return the and reset captured delta X
	 */
	public static double getAndResetCapturedDeltaX() {
		return getHandler().getAndResetCapturedX();
	}
	
	/**
	 * Gets the and reset captured delta Y.
	 * Get the delta y movement between current and previous position, then set the y delta movement back to 0 for next capture
	 * @return the and reset captured delta Y
	 */
	public static double getAndResetCapturedDeltaY() {
		return getHandler().getAndResetCapturedY();
	}
}
