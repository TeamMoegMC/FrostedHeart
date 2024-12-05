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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.lang.Lang;

import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.registries.ForgeRegistries;

class WantedFoodsGenerator {
    private final Random random;
    private final Set<Item> foodsEaten;
    private MutableComponent wantedFoodsText = Lang.translateMessage("wanted_foods");
    private final int eatenFoodsAmount;
    private final int maxGenerateAmount;
    private HashSet<Item> wantedFoods = new HashSet<>();

    public WantedFoodsGenerator(Set<Item> foodsEaten, int eatenFoodsAmount) {
        random = new Random();
        this.foodsEaten = foodsEaten;
        this.eatenFoodsAmount = eatenFoodsAmount;
        maxGenerateAmount = Math.min(eatenFoodsAmount / 10, 3);

    }
    static TagKey<Item> raw_food=ItemTags.create(FHMain.rl("raw_food"));
    static TagKey<Item> bad_food=ItemTags.create(FHMain.rl("bad_food"));
    private static boolean isNotBadFood(Item food) {
        return ForgeRegistries.ITEMS.getDelegate(food).map(t->t.is(raw_food)||t.is(bad_food)).orElse(false);
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
                wantedFoodsText.append(Lang.translateKey(food.getDescriptionId())).append(Lang.str("  "));
            }
            i++;
        }
        if (wantedFoods.isEmpty()) {
            wantedFoods = this.generate();
        }
        return wantedFoods;
    }

    public MutableComponent getWantedFoodsText() {
        return wantedFoodsText;
    }
}
