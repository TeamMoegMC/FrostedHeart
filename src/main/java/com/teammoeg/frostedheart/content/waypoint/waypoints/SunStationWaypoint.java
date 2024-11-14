package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class SunStationWaypoint extends Waypoint {
    public SunStationWaypoint() {
        super(new Vec3(0, 0, 0), "Sun Station", 0xFFFFDA64);
        this.displayName = TranslateUtils.translateWaypoint("sun_station");
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
