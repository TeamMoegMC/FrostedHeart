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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.chorda.util.io.CodecUtil;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public class ItemClue extends Clue {
	public static final MapCodec<ItemClue> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
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
    public void end(TeamDataHolder team,Research parent) {
    }

    @Override
    public String getBrief(Research parent) {
        if (consume)
            return "Submit item " + getDescriptionString(parent);
        return "Inspect item " + getDescriptionString(parent);
    }

    @Override
    public Component getDescription(Research parent) {
        Component itc = super.getDescription(parent);
        if (itc != null || stack == null)
            return itc;
        if (stack.hasNoMatchingItems())
            return null;
        return stack.getMatchingStacks()[0].getHoverName().plainCopy()
                .append(Components.str(" x" + stack.getCount()));
    }

    @Override
    public Component getName(Research parent) {
        if (name != null && !name.isEmpty())
            return super.getName(parent);
        if (consume)
            return Lang.translateKey("clue." + FHMain.MODID + ".consume_item");
        return Lang.translateKey("clue." + FHMain.MODID + ".item");
    }

    @Override
    public void init(Research parent) {
    }

    @Override
    public void start(TeamDataHolder team,Research parent) {
    }


    public int test(TeamDataHolder t,Research r, ItemStack stack) {
    	TeamResearchData trd=t.getData(FHSpecialDataTypes.RESEARCH_DATA);
        if (!trd.isClueCompleted(r,this))
            if (this.stack.test(stack)) {
            	trd.setClueCompleted(t, r, 0, consume);
                if (consume)
                    return this.stack.getCount();
            }
        return 0;
    }
}
