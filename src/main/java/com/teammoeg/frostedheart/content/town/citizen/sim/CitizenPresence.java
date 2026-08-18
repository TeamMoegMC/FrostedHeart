/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

/**
 * Defines which runtime systems should process each citizen state.
 * Sleeping remains behavior-scheduled so morning transitions still run, but
 * it has no movement, spatial-grid, or network presence.
 */
public final class CitizenPresence {

	private CitizenPresence() {
	}

	/** Whether the behavior state machine must continue receiving scheduled ticks. */
	public static boolean behaviorScheduled(int state) {
		return isValid(state);
	}

	/** Whether movement and terrain integration should process this state. */
	public static boolean movementIntegrated(int state) {
		return isValid(state) && state != CitizenState.SLEEP;
	}

	/** Whether the citizen participates in spatial queries such as separation. */
	public static boolean spatialPresent(int state) {
		return isValid(state) && state != CitizenState.SLEEP;
	}

	/** Whether clients may track, render, or interact with the citizen. */
	public static boolean networkVisible(int state) {
		return isValid(state) && state != CitizenState.SLEEP;
	}

	private static boolean isValid(int state) {
		return state >= 0 && state < CitizenState.STATE_COUNT;
	}
}
