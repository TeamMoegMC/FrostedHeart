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

package com.teammoeg.frostedheart.content.research.research.effects;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

/**
 * Reward the research team executes command
 */
public class EffectExperience extends Effect {
	public static final Codec<EffectExperience> CODEC=RecordCodecBuilder.create(t->t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
	Codec.INT.fieldOf("experience").forGetter(o->o.exp))
	.apply(t,EffectExperience::new));
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
    public FHIcon getDefaultIcon() {
        return FHIcons.getIcon(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.exp");
    }

    @Override
    public List<Component> getDefaultTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Lang.str("+" + exp));
        return tooltip;
    }

    @Override
    public boolean grant(TeamDataHolder team,TeamResearchData trd,  Player triggerPlayer, boolean isload) {
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
