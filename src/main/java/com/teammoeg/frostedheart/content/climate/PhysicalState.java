/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import net.minecraft.util.StringRepresentable;

public enum PhysicalState implements StringRepresentable{
	GAS,
	LIQUID,
	SOLID;
	public static enum StateTranslation{
		MELTING,
		SUBLIMATION,
		FREEZING,
		EVAPORATION,
		CONDENSATION,
		DEPOSITION,
		NONE;
		public static final StateTranslation[][] TRANSTION_MAP=new StateTranslation[][] {
			new StateTranslation[] {NONE,CONDENSATION,DEPOSITION},
			new StateTranslation[] {EVAPORATION,NONE,FREEZING},
			new StateTranslation[] {SUBLIMATION,MELTING,NONE}
		};
	}
	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}
	public StateTranslation translate(PhysicalState newState) {
		return StateTranslation.TRANSTION_MAP[this.ordinal()][newState.ordinal()];
	}

	public static PhysicalState fromString(String s) {
		if (s.equalsIgnoreCase("solid"))
			return SOLID;
		else if (s.equalsIgnoreCase("liquid"))
			return LIQUID;
		else
			return GAS;
	}
}
