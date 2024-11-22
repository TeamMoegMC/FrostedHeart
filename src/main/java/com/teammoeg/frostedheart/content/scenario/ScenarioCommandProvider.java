package com.teammoeg.frostedheart.content.scenario;

import net.minecraftforge.api.distmarker.Dist;

public @interface ScenarioCommandProvider {
	boolean clientOnly();
	boolean delegate();
}
