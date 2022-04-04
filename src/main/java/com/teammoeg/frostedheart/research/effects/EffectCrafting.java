package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.Item;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.List;

public class EffectCrafting extends Effect{

    List<Item> itemsToCraft;

    public EffectCrafting(Item... items) {
        name = GuiUtils.translateGui("effect.crafting");
        itemsToCraft = new ArrayList<>();
        for (Item item : items) {
            itemsToCraft.add(item);
        }
    }

    public List<Item> getItemsToCraft() {
        return itemsToCraft;
    }
    public EffectCrafting(JsonObject jo) {}
    @Override
    public void init() {

    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }

	@Override
	public JsonElement serialize() {
		return null;
	}

	@Override
	public void write(PacketBuffer buffer) {
	}
}
