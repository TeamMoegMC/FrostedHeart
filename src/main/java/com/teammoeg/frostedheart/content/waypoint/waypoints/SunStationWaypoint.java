package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3f;

public class SunStationWaypoint extends waypoint {
    public SunStationWaypoint() {
        super(new Vector3f(0, 0, 0), "Sun Station", 0xFFFFDA64);
        this.displayName = TranslateUtils.translateWaypoint("sun_station");
        this.focus = true;
    }

    public SunStationWaypoint(CompoundNBT nbt) {
        super(nbt);
    }

    public SunStationWaypoint(PacketBuffer buffer) {
        super(buffer);
    }

    @Override
    public void render(MatrixStack ms) {
        float dayTime = 0;
        if (ClientUtils.getWorld() != null) {
            dayTime = (float)Math.toRadians(ClientUtils.getWorld().func_242415_f(ClientUtils.mc().getRenderPartialTicks())*360F);
        }
        float x = (float) Math.sin(dayTime)*200000000;
        float y = (float) Math.cos(dayTime)*200000000;
        float z = (float) Math.cos(Math.toRadians((dayTime*8 % 1)*360F))*19000000;
        target.set(-x, y, z);
        super.render(ms);
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
