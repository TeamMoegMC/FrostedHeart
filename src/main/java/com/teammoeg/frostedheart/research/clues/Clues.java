package com.teammoeg.frostedheart.research.clues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketBuffer;

public class Clues {

	private Clues() {
	}
	private static Map<String,Function<JsonObject,Clue>> fromJson=new HashMap<>();
	private static List<Function<PacketBuffer,Clue>> fromPacket=new ArrayList<>();
	public static void register(String id,Function<JsonObject,Clue> j,Function<PacketBuffer,Clue> p) {
		fromJson.put(id,j);
		fromPacket.add(p);
	}
	static {
		register("custom",CustomClue::new,CustomClue::new);
		register("advancement",AdvancementClue::new,AdvancementClue::new);
		register("item",ItemClue::new,ItemClue::new);
		register("kill",KillClue::new,KillClue::new);
		register("game",MinigameClue::new,MinigameClue::new);
	}
	public static Clue read(PacketBuffer pb) {
		return fromPacket.get(pb.readVarInt()).apply(pb);
	};

	public static Clue read(JsonObject jo) {
		return fromJson.get(jo.get("type").getAsString()).apply(jo);
	};
}
