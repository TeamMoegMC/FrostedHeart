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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

/**
 * Reward the research team executes command
 */
public class EffectCommand extends Effect {
	public static final Codec<EffectCommand> CODEC=RecordCodecBuilder.create(t->t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
	Codec.list(Codec.STRING).fieldOf("rewards").forGetter(o->o.rewards))
	.apply(t,EffectCommand::new));

    List<String> rewards;

    public EffectCommand(BaseData data, List<String> rewards) {
		super(data);
		this.rewards = new ArrayList<>(rewards);
	}

	public EffectCommand(String... cmds) {
        super();
        rewards = new ArrayList<>();

        rewards.addAll(Arrays.asList(cmds));
    }

    @Override
    public String getBrief() {
        if (rewards.isEmpty())
            return "No Command";

        return "Command " + rewards.get(0) + (rewards.size() > 1 ? " ..." : "");
    }

    @Override
    public FHIcon getDefaultIcon() {
        return FHIcons.getIcon(Blocks.COMMAND_BLOCK);
    }

    @Override
    public MutableComponent getDefaultName() {
        return TranslateUtils.translateGui("effect.command");
    }

    @Override
    public List<Component> getDefaultTooltip() {
        return new ArrayList<>();
    }

    @Override
    public boolean grant(TeamResearchData team, Player triggerPlayer, boolean isload) {
        if (triggerPlayer == null || isload)
            return false;

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("p", triggerPlayer.getGameProfile().getName());

        BlockPos pos = triggerPlayer.blockPosition();
        overrides.put("x", pos.getX());
        overrides.put("y", pos.getY());
        overrides.put("z", pos.getZ());

        overrides.put("t", team.getHolder().getTeam().get().getId().toString());
        Commands cmds = FHTeamDataManager.getServer().getCommands();
        CommandSourceStack source = FHTeamDataManager.getServer().createCommandSourceStack();
        for (String s : rewards) {

            for (Map.Entry<String, Object> entry : overrides.entrySet()) {
                if (entry.getValue() != null) {
                    s = s.replace("@" + entry.getKey(), entry.getValue().toString());
                }
            }

            cmds.performPrefixedCommand(source, s);
        }

        return true;
    }

    @Override
    public void init() {

    }

    // We dont redo command, it's not possible
    @Override
    public void revoke(TeamResearchData team) {

    }

}
