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
import com.teammoeg.chorda.util.Lang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class ColumbiatWaypoint extends Waypoint {
    public ColumbiatWaypoint() {
        super(new Vec3(0, 0, 0), "Columbiat", 0xFFF6F1D5);
        this.displayName = Lang.waypoint("columbiat").component();
        this.focus = true;
    }

    public ColumbiatWaypoint(CompoundTag nbt) {
        super(nbt);
    }

    public ColumbiatWaypoint(FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void render(GuiGraphics graphics) {
        float sunAngle = 0;
        if (ClientUtils.getWorld() != null) {
            sunAngle = ClientUtils.getWorld().getSunAngle(ClientUtils.mc().getPartialTick());
        }
        float moonAngle = sunAngle + (float) Math.PI;
        float x = (float) Math.sin(moonAngle);
        float y = (float) Math.cos(moonAngle);
        float z = (float) Math.cos(moonAngle*24);
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
