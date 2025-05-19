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
}
