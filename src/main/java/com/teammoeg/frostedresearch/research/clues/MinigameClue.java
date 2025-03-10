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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.chat.Component;

public class MinigameClue extends CustomClue {
    public static final MapCodec<MinigameClue> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
            Clue.BASE_CODEC.forGetter(o -> o.getData()),
            Codec.INT.fieldOf("level").forGetter(o -> o.level)
    ).apply(t, MinigameClue::new));
    int level = 0;

    MinigameClue() {
        super();
    }

    public MinigameClue(BaseData data, int level) {
        super(data);
        this.level = level;
    }



    public MinigameClue(String nonce, String name, String desc, String hint, float contribution, boolean required, int level) {
		super(nonce, name, desc, hint, contribution, required);
		this.level = level;
	}

	@Override
    public String getBrief() {
        return "Complete game level " + this.level;
    }


    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.min(Math.max(level, 0), 3);
    }

    @Override
    public Component getName(Research parent) {
        if (name != null && !name.isEmpty())
            return super.getName(parent);
        return Lang.translateKey("clue." + FRMain.MODID + ".minigame.t" + level);
    }

}
