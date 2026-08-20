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
 * it has no movement or spatial-grid presence. Only sleepers anchored to a
 * verified bed may enter the presentation budget.
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

	/** Whether a citizen may be selected for synchronization and rendering. */
	public static boolean presentationEligible(CitizenSim sim, int index) {
		int state = sim.state[index] & 0xFF;
		if (!isValid(state))
			return false;
		return state != CitizenState.SLEEP
				|| (sim.presentationFlags[index] & CitizenSim.PRESENT_ON_VALID_BED) != 0;
	}

	/** Whether clients may perform actions against this behavior state. */
	public static boolean interactionAllowed(int state) {
		return isValid(state) && state != CitizenState.SLEEP;
	}

	private static boolean isValid(int state) {
		return state >= 0 && state < CitizenState.STATE_COUNT;
	}
}
