package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.thermopolium.data.recipes.SerializeUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkHooks;

public class HeatHandler {
    public static void openHeatScreen(ServerPlayerEntity spe, HeatEnergyNetwork vd) {
        NetworkHooks.openGui(spe, vd, e -> SerializeUtil.writeList(e, vd.data.values(), EndPointData::writeNetwork));
    }
}
