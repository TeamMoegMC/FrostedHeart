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

package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Allows the research team to use certain machines
 */
public class EffectShowCategory extends Effect {

    ResourceLocation cate;

    EffectShowCategory() {
        super();
    }

    public EffectShowCategory(JsonObject jo) {
        super(jo);
        cate = new ResourceLocation(jo.get("category").getAsString());
    }

    public EffectShowCategory(PacketBuffer pb) {
        super(pb);
        cate = pb.readResourceLocation();

    }

    public EffectShowCategory(ResourceLocation cat) {
        super();
        cate = cat;
    }

    @Override
    public String getBrief() {
        return "JEI Category " + cate.toString();
    }

    @Override
    public FHIcon getDefaultIcon() {
        return FHIcons.getIcon(Blocks.CRAFTING_TABLE);
    }

    @Override
    public IFormattableTextComponent getDefaultName() {
        return GuiUtils.translateGui("effect.category");
    }


    @Override
    public List<ITextComponent> getDefaultTooltip() {
        List<ITextComponent> tooltip = new ArrayList<>();
        return tooltip;
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
        team.categories.add(cate);
        return true;
    }


    @Override
    public void init() {
        ResearchListeners.categories.add(cate);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClick() {
        if (cate != null)
            JEICompat.showJEICategory(cate);
    }

    @Override
    public void revoke(TeamResearchData team) {
        team.categories.remove(cate);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("category", cate.toString());
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeResourceLocation(cate);
    }
}
