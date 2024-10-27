package com.teammoeg.frostedheart.content.water.item;



public class DurableDrinkContainerItem extends DrinkContainerItem {
    public DurableDrinkContainerItem( Properties properties, int capacity) {
        super( properties.durability(capacity).defaultDurability(0), capacity);
    }
}
