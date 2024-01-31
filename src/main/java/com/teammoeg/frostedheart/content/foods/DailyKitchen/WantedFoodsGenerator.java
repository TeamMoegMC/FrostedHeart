package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;

public class WantedFoodsGenerator {
    private final Random random;
    private final Set<Item> foodsEaten;
    private TextComponent wantedFoodsText = GuiUtils.translateMessage("wanted_foods");
    private final int eatenFoodsAmount;
    private final int maxGenerateAmount;
    private HashSet<Item> wantedFoods = new HashSet<>();

    public WantedFoodsGenerator(Set<Item> foodsEaten, int eatenFoodsAmount){
        random = new Random();
        this.foodsEaten = foodsEaten;
        this.eatenFoodsAmount = eatenFoodsAmount;
        maxGenerateAmount = Math.min(eatenFoodsAmount/10, 3);

    }

    private static boolean isNotBadFood(Item food){
        Set<ResourceLocation> tags = food.getTags();
        for(ResourceLocation tag : tags){
            String path = tag.getPath();
            if(path.equals("raw_food") || path.equals("bad_food")) return false;
        }
        return true;
    }

    public HashSet<Item> generate(){
        ArrayList<Integer> wantedFoodsNumber = new ArrayList<>();
        for(int i=0; i<maxGenerateAmount;){
            int randomNumber = random.nextInt(eatenFoodsAmount);
            if(!wantedFoodsNumber.contains(randomNumber)) {
                wantedFoodsNumber.add(randomNumber);
                i++;
            }
        }
        int i = 0;
        for(Item food : foodsEaten) {
            if(wantedFoodsNumber.contains(i) && (isNotBadFood(food)) ){
                wantedFoods.add(food);
                wantedFoodsText.appendSibling(food.getName()).appendSibling(new StringTextComponent("  "));
            }
            i++;
        }
        if(wantedFoods.isEmpty()){
            wantedFoods = this.generate();
        }
        return wantedFoods;
    }

    public TextComponent getWantedFoodsText() {
        return wantedFoodsText;
    }
}
