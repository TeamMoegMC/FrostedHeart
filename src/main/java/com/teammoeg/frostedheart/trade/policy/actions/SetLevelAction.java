package com.teammoeg.frostedheart.trade.policy.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import net.minecraft.network.PacketBuffer;

public class SetLevelAction extends AbstractAction {
    int value;

    public SetLevelAction(JsonObject jo) {
        value = jo.get("level").getAsInt();
    }

    public SetLevelAction(int value) {
        this.value = value;
    }

    public SetLevelAction(PacketBuffer buffer) {
        value = buffer.readVarInt();
    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.addProperty("level", value);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeVarInt(value);
    }

    @Override
    public void deal(FHVillagerData data, int num) {
        data.setTradelevel(value);
    }


}
