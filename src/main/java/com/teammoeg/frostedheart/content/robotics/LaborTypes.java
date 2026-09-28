package com.teammoeg.frostedheart.content.robotics;

import com.teammoeg.frostedheart.content.robotics.labor.LaborType;

/**
 * 已注册的劳工类型。劳工点数按类型分别计算，每种类型有一个独立的池。
 */
public class LaborTypes {

	/** 通用劳工点数。 */
	public static final LaborType WORKER = LaborType.register("worker");

	public static void init() {
	}
}
