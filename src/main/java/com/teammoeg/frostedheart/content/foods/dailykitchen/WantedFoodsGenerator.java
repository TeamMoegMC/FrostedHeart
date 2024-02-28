/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.foods.dailykitchen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.teammoeg.frostedheart.util.client.GuiUtils;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.capability.ItemFluidContainer;

class WantedFoodsGenerator {
    private final Random random;
    private final Set<Item> foodsEaten;
    private TextComponent wantedFoodsText = GuiUtils.translateMessage("wanted_foods");
    private final int eatenFoodsAmount;
    private final int maxGenerateAmount;
    private HashSet<Item> wantedFoods = new HashSet<>();

    public WantedFoodsGenerator(Set<Item> foodsEaten, int eatenFoodsAmount) {
        random = new Random();
        this.foodsEaten = foodsEaten;
        this.eatenFoodsAmount = eatenFoodsAmount;
        maxGenerateAmount = Math.min(eatenFoodsAmount / 10, 3);

    }

    private static boolean isNotBadFood(Item food) {
        Set<ResourceLocation> tags = food.getTags();
        for (ResourceLocation tag : tags) {
            String path = tag.getPath();
            if (path.equals("raw_food") || path.equals("bad_food")) return false;
        }
        return true;
    }

    public HashSet<Item> generate() {
        ArrayList<Integer> wantedFoodsNumber = new ArrayList<>();
        for (int i = 0; i < maxGenerateAmount; ) {
            int randomNumber = random.nextInt(eatenFoodsAmount);
            if (!wantedFoodsNumber.contains(randomNumber)) {
                wantedFoodsNumber.add(randomNumber);
                i++;
            }
        }
        int i = 0;
        for (Item food : foodsEaten) {
            if (wantedFoodsNumber.contains(i) && (isNotBadFood(food)) && !(food instanceof ItemFluidContainer/*Don't eat thermos!*/)) {
                wantedFoods.add(food);
                wantedFoodsText.appendSibling(new TranslationTextComponent(food.getTranslationKey())).appendSibling(GuiUtils.str("  "));
            }
            i++;
        }
        if (wantedFoods.isEmpty()) {
            wantedFoods = this.generate();
        }
        return wantedFoods;
    }

    public TextComponent getWantedFoodsText() {
        return wantedFoodsText;
    }
}
