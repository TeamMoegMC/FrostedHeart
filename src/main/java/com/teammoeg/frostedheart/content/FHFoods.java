package com.teammoeg.frostedheart.content;

import net.minecraft.item.Food;

public class FHFoods {
    public static final Food VEGETABLE_SAWDUST_SOUP = buildStew(6); // 掺杂了木屑的蔬菜汤
    public static final Food RYE_SAWDUST_PORRIDGE = buildStew(6); // 掺杂了木屑的黑麦面糊
    public static final Food RYE_BREAD = (new Food.Builder()).hunger(5).saturation(0.6F).build();

    private static Food buildStew(int hunger) {
        return (new Food.Builder()).hunger(hunger).saturation(0.6F).build();
    }
}
