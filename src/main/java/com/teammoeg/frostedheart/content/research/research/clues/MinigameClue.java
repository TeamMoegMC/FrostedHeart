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
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.network.chat.Component;

public class MinigameClue extends CustomClue {
	public static final Codec<MinigameClue> CODEC=RecordCodecBuilder.create(t->t.group(
		Clue.BASE_CODEC.forGetter(o->o.getData()),
		Codec.INT.fieldOf("level").forGetter(o->o.level)
		).apply(t,MinigameClue::new));
    private int level = 0;

    MinigameClue() {
        super();
    }

    public MinigameClue(BaseData data, int level) {
		super(data);
		this.level = level;
	}

	public MinigameClue(float contribution) {
        super("", contribution);
    }

    @Override
    public String getBrief(Research parent) {
        return "Complete game level " + this.level;
    }


    public int getLevel() {
        return level;
    }

    @Override
    public Component getName(Research parent) {
        if (name != null && !name.isEmpty())
            return super.getName(parent);
        return TranslateUtils.translate("clue." + FHMain.MODID + ".minigame.t" + level);
    }

    public void setLevel(int level) {
        this.level = Math.min(Math.max(level, 0), 3);
    }

}
