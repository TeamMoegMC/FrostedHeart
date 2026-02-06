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

package com.teammoeg.frostedheart.content.scenario.runner.trigger;

import com.teammoeg.frostedheart.content.scenario.runner.target.SingleExecuteTargetTrigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class MovementTrigger extends SingleExecuteTargetTrigger {
	Vec3 pos;
	public MovementTrigger(Player pe) {
		super(null);
		this.test=t->t.player().position().distanceToSqr(this.pos)>0.25;
		pos=pe.position();
	}

}
