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

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class EffectCustom extends Effect {
	public static final MapCodec<EffectCustom> CODEC = RecordCodecBuilder
			.mapCodec(t -> t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData)).apply(t, EffectCustom::new));

	EffectCustom() {
		super();
	}

	public EffectCustom(BaseData data) {
		super(data);

	}

	@Override
	public String getBrief() {
		return super.name;
	}

	@Override
	public CIcon getDefaultIcon() {
		return CIcons.nop();
	}

	@Override
	public MutableComponent getDefaultName() {
		return Components.str("");
	}

	@Override
	public List<Component> getDefaultTooltip() {
		return List.of();
	}

	@Override
	public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
		return true;
	}

	@Override
	public void init() {
	}

	@Override
	public void revoke(TeamResearchData team) {
	}

}
