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

package com.teammoeg.frostedresearch.research.clues;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementClue extends TickListenerClue {
    public static final MapCodec<AdvancementClue> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
            ListenerClue.BASE_CODEC.forGetter(o -> o.getData()),
            ResourceLocation.CODEC.fieldOf("advancement").forGetter(o -> o.advancement),
            Codec.STRING.optionalFieldOf("criterion","").forGetter(o -> o.criterion)
    ).apply(t, AdvancementClue::new));
    ResourceLocation advancement = new ResourceLocation("minecraft:story/root");
    String criterion = "";

    public AdvancementClue() {
        super();
    }

    public AdvancementClue(BaseData data, ResourceLocation advancement, String criterion) {
        super(data);
        this.advancement = advancement;
        this.criterion = criterion;
    }

    public AdvancementClue(String name, float contribution) {
        super(name, contribution);
    }




	@Override
    public String getBrief() {
    	ClientAdvancements cam = ClientUtils.getPlayer().connection.getAdvancements();
        Advancement adv = cam.getAdvancements().get(advancement);
        if (adv != null)
            return "Advancement " +adv.getChatComponent().getString();
        return "Advancement none" ;
    }

    public AdvancementClue(String nonce, String name, String desc, String hint, float contribution, boolean required, boolean alwaysOn, Pair<Advancement,String> advancement) {
		super(nonce, name, desc, hint, contribution, required, alwaysOn);
		if(advancement!=null) {
			if(advancement.getFirst()!=null)
			this.advancement = advancement.getFirst().getId();
			this.criterion = advancement.getSecond();
		}
		
	}
	@Override
    public Component getDescription(Research parent) {
        Component itc = super.getDescription(parent);
        if (itc != null) return itc;
        ClientAdvancements cam = ClientUtils.getPlayer().connection.getAdvancements();
        Advancement adv = cam.getAdvancements().get(advancement);
        if (adv != null)
            return adv.getChatComponent();
        else
            return null;

    }


    @Override
    public Component getName(Research parent) {
        if (name != null && !name.isEmpty())
            return super.getName(parent);
        return Lang.translateKey("clue." + FRMain.MODID + ".advancement");
    }

    @Override
    public boolean isCompleted(TeamResearchData t, ServerPlayer player) {
        Advancement a = player.server.getAdvancements().getAdvancement(advancement);
        if (a == null) {
            return false;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(a);

        if (criterion.isEmpty()) {
            return progress.isDone();
        }
        CriterionProgress criterionProgress = progress.getCriterion(criterion);
        return criterionProgress != null && criterionProgress.isDone();
    }
}
