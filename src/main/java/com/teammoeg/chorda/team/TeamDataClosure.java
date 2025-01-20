package com.teammoeg.chorda.team;

import java.util.UUID;
import java.util.function.Supplier;

public record TeamDataClosure<T extends SpecialData>(TeamDataHolder team,SpecialDataType<T> type) implements Supplier<T>{
	public T get() {
		return team.getData(type);
	}
	public UUID getId() {
		return team.getId();
	}
}
