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
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class KillClue extends ListenerClue {
	public static final Codec<KillClue> CODEC=RecordCodecBuilder.create(t->t.group(
		ListenerClue.BASE_CODEC.forGetter(o->o.getData()),
		CodecUtil.registryCodec(()->BuiltInRegistries.ENTITY_TYPE).fieldOf("entity").forGetter(o->o.type)
		).apply(t,KillClue::new));
    EntityType<?> type;

    KillClue() {
        super();
    }


    public KillClue(EntityType<?> t, float contribution) {
        super("", "", "", contribution);
    }

    public KillClue(BaseData data, EntityType<?> type) {
		super(data);
		this.type = type;
	}

    @Override
    public String getBrief() {
        return "Kill " + getDescriptionString();
    }

    @Override
    public Component getDescription() {
        Component itc = super.getDescription();
        if (itc != null || type == null) return itc;
        return type.getDescription();
    }

    @Override
    public String getId() {
        return "kill";
    }

    @Override
    public Component getName() {
        if (name != null && !name.isEmpty())
            return super.getName();
        return TranslateUtils.translate("clue." + FHMain.MODID + ".kill");
    }

    @Override
    public void initListener(TeamDataHolder t) {
        ResearchListeners.getKillClues().add(this, t.getId());
    }

    public boolean isCompleted(TeamResearchData trd, LivingEntity e) {
        if (type != null && type.equals(e.getType())) {
            this.setCompleted(trd, true);
            return true;
        }
        return false;
    }

    @Override
    public void removeListener(TeamDataHolder t) {
        ResearchListeners.getKillClues().remove(this, t.getId());
    }

}
