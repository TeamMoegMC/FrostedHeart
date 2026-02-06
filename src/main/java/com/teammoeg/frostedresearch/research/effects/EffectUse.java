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
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {
    public static final MapCodec<EffectUse> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
                    Codec.list(ForgeRegistries.BLOCKS.getCodec()).fieldOf("blocks").forGetter(o -> o.blocks))
            .apply(t, EffectUse::new));
    List<Block> blocks;

    public EffectUse(BaseData data, List<Block> blocks) {
        super(data);
        this.blocks = new ArrayList<>(blocks);
    }

    EffectUse() {
        super();
        this.blocks = new ArrayList<>();
    }

    public EffectUse(Block... blocks) {
        super();
        this.blocks = new ArrayList<>();
        this.blocks.addAll(Arrays.asList(blocks));
    }

    @Override
    public String getBrief() {
        if (blocks.isEmpty())
            return "Use nothing";
        return "Use " + blocks.get(0).getName().getString() + (blocks.size() > 1 ? " ..." : "");
    }

    @Override
    public CIcon getDefaultIcon() {
        return CIcons.getIcon(CIcons.getIcon(blocks.toArray(new Block[0])), CIcons.getDelegateIcon("hand"));
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.use");
    }


    @Override
    public List<Component> getDefaultTooltip() {
        List<Component> tooltip = new ArrayList<>();
        for (Block b : blocks) {
            tooltip.add(b.getName());
        }

        return tooltip;
    }

    @Override
    public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
        trd.getUnlockList(ResearchHooks.BLOCK_UNLOCK_LIST).addAll(blocks);
        return true;
    }

    @Override
    public void init() {
        ResearchHooks.getLockList(ResearchHooks.BLOCK_UNLOCK_LIST).addAll(blocks);
    }

    @Override
    public void revoke(TeamResearchData team) {
        team.getUnlockList(ResearchHooks.BLOCK_UNLOCK_LIST).removeAll(blocks);
    }
}
