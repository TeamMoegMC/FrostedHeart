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

package com.teammoeg.frostedresearch.research.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.ResearchListeners;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows the research team to use certain machines
 */
public class EffectShowCategory extends Effect {
    public static final MapCodec<EffectShowCategory> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
                    ResourceLocation.CODEC.fieldOf("category").forGetter(o -> o.cate))
            .apply(t, EffectShowCategory::new));
    ResourceLocation cate;

    public EffectShowCategory(BaseData data, ResourceLocation cate) {
        super(data);
        this.cate = cate;
    }

    public EffectShowCategory(String name, List<String> tooltip, ResourceLocation cate) {
        super(name, tooltip);
        this.cate = cate;
    }

    EffectShowCategory() {
        super();
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
    public CIcon getDefaultIcon() {
        return CIcons.getIcon(Blocks.CRAFTING_TABLE);
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.category");
    }


    @Override
    public List<Component> getDefaultTooltip() {
        return new ArrayList<>();
    }

    @Override
    public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
        trd.categories.add(cate);
        return true;
    }


    @Override
    public void init() {
        ResearchListeners.categories.add(cate);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClick(ResearchData data) {
        if (cate != null)
            JEICompat.showJEICategory(cate);
    }

    @Override
    public void revoke(TeamResearchData team) {
        team.categories.remove(cate);
    }
}
