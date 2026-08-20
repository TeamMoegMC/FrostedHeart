/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import com.jozufozu.flywheel.api.InstanceData;

/** Compact snapshot state consumed by the Flywheel citizen vertex shader. */
final class CitizenInstanceData extends InstanceData {

	byte blockLight;
	byte skyLight;
	float pos0X;
	float pos0Y;
	float pos0Z;
	float pos1X;
	float pos1Y;
	float pos1Z;
	float snapshotTime;
	float snapshotDuration;
	float velocityX;
	float velocityZ;
	float yawStart;
	float yawDelta;
	float yawTime;
	byte moving;
	byte sleeping;
	byte phase;
	byte age;
}
