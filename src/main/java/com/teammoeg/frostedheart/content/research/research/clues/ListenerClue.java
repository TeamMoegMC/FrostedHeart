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

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.team.TeamDataHolder;

import net.minecraft.network.PacketBuffer;

/**
 * Clue with listener trigger
 */
public abstract class ListenerClue extends Clue {
    public boolean alwaysOn;

    public ListenerClue() {
        super();
    }

    public ListenerClue(JsonObject jo) {
        super(jo);
        alwaysOn = jo.get("always").getAsBoolean();
    }

    public ListenerClue(PacketBuffer pb) {
        super(pb);
        alwaysOn = pb.readBoolean();
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
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("always", alwaysOn);
        return jo;
    }

    @Override
    public void start(TeamDataHolder team) {
        if (!alwaysOn)
            initListener(team);

    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeBoolean(alwaysOn);
    }

}
