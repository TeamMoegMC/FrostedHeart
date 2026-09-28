package com.teammoeg.frostedheart.content.robotics;

import com.teammoeg.frostedheart.content.robotics.labor.MachineType;
import com.teammoeg.frostedheart.content.robotics.labor.MachineType.BaseMachineType;

public class MachineTypes {

	/** 物流核心：使用通用劳工，最大等级 10，每级 5 点。 */
	public static final MachineType LOGISTIC = MachineType.register(
			new BaseMachineType(LaborTypes.WORKER, 10, 5), "logistic");

	public static void init() {
		LaborTypes.init();
	}
}
