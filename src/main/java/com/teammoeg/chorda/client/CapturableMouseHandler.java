package com.teammoeg.chorda.client;

public interface CapturableMouseHandler {

	boolean isMouseCaptured();

	void setCaptureMouse(boolean captureMouse);

	double getAndResetCapturedX();

	double getAndResetCapturedY();

}