package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.thermopolium.data.recipes.SerializeUtil;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkHooks;

public class HeatHandler {
    public static void openHeatScreen(ServerPlayer spe, HeatEnergyNetwork vd) {
        NetworkHooks.openGui(spe, vd, e -> SerializeUtil.writeList(e, vd.data.values(), EndPointData::writeNetwork));
    }
}
