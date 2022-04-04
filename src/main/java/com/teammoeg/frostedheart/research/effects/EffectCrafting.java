package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class EffectCrafting extends Effect {

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

    @Override
    public void init() {

    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
