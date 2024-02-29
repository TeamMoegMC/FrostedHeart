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

package com.teammoeg.frostedheart.research.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;

public class ItemClue extends Clue {
    boolean consume;
    IngredientWithSize stack;

    ItemClue() {
        super();
    }

    public ItemClue(JsonObject jo) {
        super(jo);
        stack = IngredientWithSize.deserialize(jo.get("item"));
        if (jo.has("consume"))
            consume = jo.get("consume").getAsBoolean();
    }

    public ItemClue(PacketBuffer pb) {
        super(pb);
        stack = IngredientWithSize.read(pb);
        consume = pb.readBoolean();
    }

    public ItemClue(String name, String desc, String hint, float contribution, IngredientWithSize stack) {
        super(name, desc, hint, contribution);
        this.stack = stack;
    }

    @Override
    public void end(Team team) {
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
        return stack.getMatchingStacks()[0].getDisplayName().copyRaw()
                .appendSibling(GuiUtils.str(" x" + stack.getCount()));
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
            return GuiUtils.translate("clue." + FHMain.MODID + ".consume_item");
        return GuiUtils.translate("clue." + FHMain.MODID + ".item");
    }

    @Override
    public void init() {
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.add("item", stack.serialize());
        if (consume)
            jo.addProperty("consume", consume);
        return jo;
    }

    @Override
    public void start(Team team) {
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

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        stack.write(buffer);
        buffer.writeBoolean(consume);
    }
}
