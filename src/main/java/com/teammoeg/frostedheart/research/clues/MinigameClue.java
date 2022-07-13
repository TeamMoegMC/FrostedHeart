package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;

public class MinigameClue extends CustomClue {
	private int level=0;
	public MinigameClue(float contribution) {
		super("@clue." + FHMain.MODID + ".minigame", contribution);
	}

	public MinigameClue(JsonObject jo) {
		super(jo);
		setLevel(jo.get("level").getAsInt());
	}

	public MinigameClue(PacketBuffer pb) {
		super(pb);
		setLevel(pb.readVarInt());
	}

	MinigameClue() {
		super();
	}
	@Override
	public ITextComponent getName() {
		if (name != null && !name.isEmpty())
			return super.getName();
		return GuiUtils.translate("clue." + FHMain.MODID + ".minigame");
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
		jo.addProperty("level", getLevel());
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeVarInt(getLevel());
		
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = Math.min(Math.max(level,0),3);
	}
	
}
