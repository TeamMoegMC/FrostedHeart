/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.research.clues;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public class ItemClue extends Clue {
	public static final Codec<ItemClue> CODEC=RecordCodecBuilder.create(t->t.group(
		Clue.BASE_CODEC.forGetter(o->o.getData()),
		CodecUtil.defaultValue(Codec.BOOL, false).fieldOf("consume").forGetter(o->o.consume),
		CodecUtil.INGREDIENT_SIZE_CODEC.fieldOf("item").forGetter(o->o.stack)
		).apply(t,ItemClue::new));
	
    boolean consume;
    IngredientWithSize stack;
    
    ItemClue() {
        super();
    }

    public ItemClue(BaseData data, boolean consume, IngredientWithSize stack) {
		super(data);
		this.consume = consume;
		this.stack = stack;
	}



    public ItemClue(String name, String desc, String hint, float contribution, IngredientWithSize stack) {
        super(name, desc, hint, contribution);
        this.stack = stack;
    }

    @Override
    public void end(TeamDataHolder team) {
    }

    @Override
    public String getBrief() {
        if (consume)
            return "Submit item " + getDescriptionString();
        return "Inspect item " + getDescriptionString();
    }

    @Override
    public ITextComponent getDescription() {
        ITextComponent itc = super.getDescription();
        if (itc != null || stack == null)
            return itc;
        if (stack.hasNoMatchingItems())
            return null;
        return stack.getMatchingStacks()[0].getHoverName().plainCopy()
                .append(TranslateUtils.str(" x" + stack.getCount()));
    }

    @Override
    public String getId() {
        return "item";
    }

    @Override
    public ITextComponent getName() {
        if (name != null && !name.isEmpty())
            return super.getName();
        if (consume)
            return TranslateUtils.translate("clue." + FHMain.MODID + ".consume_item");
        return TranslateUtils.translate("clue." + FHMain.MODID + ".item");
    }

    @Override
    public void init() {
    }

    @Override
    public void start(TeamDataHolder team) {
    }


    public int test(TeamResearchData t, ItemStack stack) {
        if (!this.isCompleted(t))
            if (this.stack.test(stack)) {
                this.setCompleted(t, true);
                if (consume)
                    return this.stack.getCount();
            }
        return 0;
    }
}
