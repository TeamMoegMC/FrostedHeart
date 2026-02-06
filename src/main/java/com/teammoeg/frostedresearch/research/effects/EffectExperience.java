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

package com.teammoeg.frostedresearch.research.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward the research team executes command
 */
public class EffectExperience extends Effect {
    public static final MapCodec<EffectExperience> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
                    Codec.INT.fieldOf("experience").forGetter(o -> o.exp))
            .apply(t, EffectExperience::new));
    int exp;

    public EffectExperience(BaseData data, int exp) {
        super(data);
        this.exp = exp;
    }


    public EffectExperience(int xp) {
        super();
        exp = xp;
    }


    @Override
    public String getBrief() {

        return "Experience " + exp;
    }

    @Override
    public CIcon getDefaultIcon() {
        return CIcons.getIcon(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.exp");
    }

    @Override
    public List<Component> getDefaultTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Components.str("+" + exp));
        return tooltip;
    }

    @Override
    public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
        if (triggerPlayer == null || isload)
            return false;

        triggerPlayer.giveExperiencePoints(exp);

        return true;
    }

    @Override
    public void init() {

    }

    @Override
    public void revoke(TeamResearchData team) {

    }

}
