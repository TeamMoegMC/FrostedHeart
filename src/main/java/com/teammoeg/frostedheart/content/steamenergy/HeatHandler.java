package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.chorda.io.SerializeUtil;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;

public class HeatHandler {
    public static void openHeatScreen(ServerPlayer spe, HeatNetwork network) {
        NetworkHooks.openScreen(spe, network, e -> SerializeUtil.writeList(e, network.getEndpoints(),
                HeatEndpoint::writeNetwork));
    }
}
