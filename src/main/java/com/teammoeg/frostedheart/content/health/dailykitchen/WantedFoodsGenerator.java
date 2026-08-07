/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.health.dailykitchen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.world.item.Item;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fluids.capability.ItemFluidContainer;

class WantedFoodsGenerator {
    private final Random random;
    private final Set<Item> foodsEaten;
    private MutableComponent wantedFoodsText = Lang.translateMessage("wanted_foods");
    private final int eatenFoodsAmount;
    private final int maxGenerateAmount;
    /** 单次抽取落空时的最大重试次数，防止极端随机/过滤导致无限递归 / max retry attempts when a single draw comes up empty, prevents infinite recursion */
    private static final int MAX_GENERATE_ATTEMPTS = 3;

    public WantedFoodsGenerator(Set<Item> foodsEaten, int eatenFoodsAmount) {
        random = new Random();
        this.foodsEaten = foodsEaten;
        this.eatenFoodsAmount = eatenFoodsAmount;
        maxGenerateAmount = Math.min(eatenFoodsAmount / 10, 3);

    }

    /**
     * 随机抽取 1-3 种玩家吃过且可推荐的正常食物作为"今日想吃的菜"。
     * 生食/坏食已在写入 foodsEaten 时过滤（见 {@link WantedFoodCapability#addEatenFood}），
     * 此处仅剩流体容器等少量过滤可能导致单次落空，故有限重试后仍为空则返回空集，
     * 由调用方静默处理（当天不显示想吃的菜），杜绝递归栈溢出。
     * <p>
     * Randomly picks 1-3 kinds of recommendable normal foods the player has eaten as
     * today's wanted foods. Raw/bad foods are already filtered when writing into
     * foodsEaten (see {@link WantedFoodCapability#addEatenFood}), so a single draw can
     * only come up empty due to residual filters like fluid containers; retrying a
     * bounded number of times and then returning an empty set lets the caller stay
     * silent (no wanted foods shown that day) instead of recursing into a stack overflow.
     *
     * @return 想吃的食物集合（可能为空）/ the wanted food set (possibly empty)
     */
    public HashSet<Item> generate() {
        for (int attempt = 0; attempt < MAX_GENERATE_ATTEMPTS; attempt++) {
            HashSet<Item> result = generateOnce();
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new HashSet<>();
    }

    /**
     * 单次抽取：随机选中 maxGenerateAmount 个不重复索引，取对应位置食物中可推荐的部分。
     * <p>
     * Single draw: randomly selects non-repeated indices and keeps the recommendable
     * foods at those positions.
     *
     * @return 本次抽到的正常食物集合（可能为空）/ the recommendable foods drawn this time (possibly empty)
     */
    private HashSet<Item> generateOnce() {
        HashSet<Item> result = new HashSet<>();
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
            if (wantedFoodsNumber.contains(i) && (WantedFoodCapability.isNormalFood(food)) && !(food instanceof ItemFluidContainer/*Don't eat thermos!*/)) {
                result.add(food);
                wantedFoodsText.append(Lang.translateKey(food.getDescriptionId())).append(Components.str("  "));
            }
            i++;
        }
        return result;
    }

    public MutableComponent getWantedFoodsText() {
        return wantedFoodsText;
    }

    /**
     * 根据已生成的食物集合组装"今日想吃的菜"提示文本（不重新生成）。
     * 供玩家登录时重发当天的想吃的菜使用，格式与 {@link #generate()} 生成时一致。
     * <p>
     * Builds the "wanted foods" message text from an already generated food set (no
     * re-generation). Used to re-send today's wanted foods on player login; format
     * matches the one produced by {@link #generate()}.
     *
     * @param foods 已生成的食物集合 / the already generated food set
     * @return 提示文本 / the message text
     */
    static MutableComponent buildWantedFoodsText(Set<Item> foods) {
        MutableComponent text = Lang.translateMessage("wanted_foods");
        for (Item food : foods) {
            text.append(Lang.translateKey(food.getDescriptionId())).append(Components.str("  "));
        }
        return text;
    }
}
