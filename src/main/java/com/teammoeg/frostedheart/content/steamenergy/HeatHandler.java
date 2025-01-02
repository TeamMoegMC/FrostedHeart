package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.util.io.SerializeUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;

public class HeatHandler {
    public static void openHeatScreen(ServerPlayer spe, HeatEnergyNetwork vd) {
        NetworkHooks.openScreen(spe, vd, e -> SerializeUtil.writeList(e, vd.data.values(), EndPointData::writeNetwork));
    }
}
