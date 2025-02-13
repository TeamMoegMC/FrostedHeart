package com.teammoeg.chorda.menu.slots;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;

public class SlotProcessValue {
	CDataSlot<Integer> PROCESS;
	CDataSlot<Integer> PROCESS_MAX;
	public SlotProcessValue(CBaseMenu menu) {
		PROCESS=CCustomMenuSlot.SLOT_INT.create(menu);
		PROCESS_MAX=CCustomMenuSlot.SLOT_INT.create(menu);
	}
	public int getProcess() {
		return PROCESS.getValue();
	}
	public int getProcessMax() {
		return PROCESS_MAX.getValue();
	}
	public void setProcess(Supplier<Integer> getter,Consumer<Integer> setter) {
		PROCESS.bind(getter, setter);
	}
	public void setProcessMax(Supplier<Integer> getter,Consumer<Integer> setter) {
		PROCESS_MAX.bind(getter, setter);
	}
	public void setProcess(Supplier<Integer> getter) {
		PROCESS.bind(getter);
	}
	public void setProcessMax(Supplier<Integer> getter) {
		PROCESS_MAX.bind(getter);
	}

}
