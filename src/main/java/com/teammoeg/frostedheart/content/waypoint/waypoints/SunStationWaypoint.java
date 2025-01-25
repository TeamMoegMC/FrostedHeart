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

package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.Lang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class SunStationWaypoint extends Waypoint {
    public SunStationWaypoint() {
        super(new Vec3(0, 0, 0), "Sun Station", 0xFFFFDA64);
        this.displayName = Lang.waypoint("sun_station").component();
        this.focus = true;
    }

    public SunStationWaypoint(CompoundTag nbt) {
        super(nbt);
    }

    public SunStationWaypoint(FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void render(GuiGraphics graphics) {
        float sunAngle = 0;
        if (ClientUtils.getWorld() != null) {
            sunAngle = ClientUtils.getWorld().getSunAngle(ClientUtils.mc().getPartialTick());
        }
        float x = (float) Math.sin(sunAngle);
        float y = (float) Math.cos(sunAngle);
        float z = (float) Math.cos(sunAngle*24);
        target = new Vec3(-x*200000000, y*200000000, z*19000000);
        super.render(graphics);
    }

    @Override
    public void updateInfos() {
        addInfoLine(displayName, -1);
    }

    @Override
    public double getDistance() {
        return -1;
    }
}
