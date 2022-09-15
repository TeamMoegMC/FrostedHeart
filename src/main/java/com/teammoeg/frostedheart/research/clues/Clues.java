package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.SerializerRegistry;
import net.minecraft.network.PacketBuffer;

import java.util.function.Function;

public class Clues {
	private static SerializerRegistry<Clue> registry=new SerializerRegistry<>();
    private Clues() {
    }


    public static void register(Class<? extends Clue> cls,String id, Function<JsonObject, Clue> j, Function<PacketBuffer, Clue> p) {
    	registry.register(cls,id,j,p);
    }

    static {
        register(CustomClue.class,"custom", CustomClue::new, CustomClue::new);
        register(AdvancementClue.class,"advancement", AdvancementClue::new, AdvancementClue::new);
        register(ItemClue.class,"item", ItemClue::new, ItemClue::new);
        register(KillClue.class,"kill", KillClue::new, KillClue::new);
        register(MinigameClue.class,"game", MinigameClue::new, MinigameClue::new);
    }
    public static void writeId(Clue e,PacketBuffer pb) {
    	registry.writeId(pb, e);
    }
    public static Clue read(PacketBuffer pb) {
    	return registry.read(pb);
    }
    public static Clue read(JsonObject jo) {
        return registry.deserialize(jo);
    }
}
