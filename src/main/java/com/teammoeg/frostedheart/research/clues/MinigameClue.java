package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.network.PacketBuffer;

public class MinigameClue extends CustomClue {

	public MinigameClue(float contribution) {
		super("@clue." + FHMain.MODID + ".minigame", contribution);
	}

	public MinigameClue(JsonObject jo) {
		super(jo);
	}

	public MinigameClue(PacketBuffer pb) {
		super(pb);
	}

	@Override
	public String getType() {
		return "game";
	}

	@Override
	public int getIntType() {
		return 4;
	}
}
