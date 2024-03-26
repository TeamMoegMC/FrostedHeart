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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

/**
 * Clue with listener trigger
 */
public abstract class ListenerClue extends Clue {
    public boolean alwaysOn;
    public static class BaseData extends Clue.BaseData{
    	public BaseData(String name, String desc, String hint, String nonce, boolean required, float contribution, boolean alwaysOn) {
			super(name, desc, hint, nonce, required, contribution);
			this.alwaysOn = alwaysOn;
		}

		boolean alwaysOn;
    }
	public static final MapCodec<BaseData> BASE_CODEC=RecordCodecBuilder.mapCodec(t->
	t.group(
		CodecUtil.defaultValue(Codec.STRING,"").fieldOf("name").forGetter(o->o.name),
		CodecUtil.defaultValue(Codec.STRING,"").fieldOf("desc").forGetter(o->o.desc),
		CodecUtil.defaultValue(Codec.STRING,"").fieldOf("hint").forGetter(o->o.hint),
		Codec.STRING.fieldOf("id").forGetter(o->o.nonce),
		Codec.BOOL.fieldOf("required").forGetter(o->o.required),
		Codec.FLOAT.fieldOf("value").forGetter(o->o.contribution),
		Codec.BOOL.fieldOf("always").forGetter(o->o.alwaysOn)).apply(t, BaseData::new));
    public ListenerClue() {
        super();
    }


    public ListenerClue(BaseData data) {
		super(data);
		this.alwaysOn=data.alwaysOn;
	}
    
    public BaseData getData() {
    	return new BaseData(name, desc, hint, nonce, required, contribution, alwaysOn);
    }

	public ListenerClue(String name, float contribution) {
        super(name, contribution);
    }

    public ListenerClue(String name, String desc, String hint, float contribution) {
        super(name, desc, hint, contribution);
    }

    @Override
    public void end(TeamDataHolder team) {
        if (!alwaysOn)
            removeListener(team);
    }

    @Override
    public void init() {
        if (alwaysOn)
            initListener(null);
    }

    public abstract void initListener(TeamDataHolder t);

    public abstract void removeListener(TeamDataHolder t);


    @Override
    public void start(TeamDataHolder team) {
        if (!alwaysOn)
            initListener(team);

    }

}
