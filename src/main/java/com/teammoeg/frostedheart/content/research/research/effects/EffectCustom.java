package com.teammoeg.frostedheart.content.research.research.effects;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;

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
