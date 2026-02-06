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
