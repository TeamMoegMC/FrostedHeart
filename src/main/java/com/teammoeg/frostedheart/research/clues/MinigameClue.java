package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.network.PacketBuffer;

public class MinigameClue extends CustomClue {
	public int level=1;
	public MinigameClue(float contribution) {
		super("@clue." + FHMain.MODID + ".minigame", contribution);
	}

	public MinigameClue(JsonObject jo) {
		super(jo);
		level=jo.get("level").getAsInt();
	}

	public MinigameClue(PacketBuffer pb) {
		super(pb);
		level=pb.readVarInt();
	}

	@Override
	public String getId() {
		return "game";
	}

	@Override
	public int getIntType() {
		return 4;
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		jo.addProperty("level", level);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeVarInt(level);
		
	}
}
