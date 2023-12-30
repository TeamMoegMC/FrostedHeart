package com.teammoeg.frostedheart.trade.policy;

import com.google.gson.JsonElement;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.network.PacketBuffer;

public class LevelData implements Writeable {
	int min;
	int max;
	int level;
	public LevelData(int min, int max, int level) {
		super();
		this.min = min;
		this.max = max;
		this.level = level;
	}

	@Override
	public JsonElement serialize() {
		return null;
	}

	@Override
	public void write(PacketBuffer buffer) {
	}

}
