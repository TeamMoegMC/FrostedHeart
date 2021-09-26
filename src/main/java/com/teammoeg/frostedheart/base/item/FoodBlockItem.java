package com.teammoeg.frostedheart.base.item;

import net.minecraft.block.Block;
import net.minecraft.item.Food;

public class FoodBlockItem extends FHBlockItem {
    public FoodBlockItem(Block block, Properties props, Food food) {
        super(block, props.food(food));
    }
}
