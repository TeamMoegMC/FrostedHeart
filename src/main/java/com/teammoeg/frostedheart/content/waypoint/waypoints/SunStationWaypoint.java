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
        float dayTime = 0;
        if (ClientUtils.getWorld() != null) {
            dayTime = (float)Math.toRadians(ClientUtils.getWorld().getSunAngle(ClientUtils.mc().getPartialTick())*360F);
        }
        float x = (float) Math.sin(dayTime)*200000000;
        float y = (float) Math.cos(dayTime)*200000000;
        float z = (float) Math.cos(Math.toRadians((dayTime*8 % 1)*360F))*19000000;
        target = new Vec3(-x, y, z);
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
