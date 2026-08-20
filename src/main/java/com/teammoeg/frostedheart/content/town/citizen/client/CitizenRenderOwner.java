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

/** Exclusive render owner for one citizen in one client frame. */
enum CitizenRenderOwner {
	DETAILED_ENTITY,
	BODY_BATCH,
	BILLBOARD_BATCH,
	NONE
}
