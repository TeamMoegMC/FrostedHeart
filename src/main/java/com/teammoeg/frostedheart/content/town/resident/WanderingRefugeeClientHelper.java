package com.teammoeg.frostedheart.content.town.resident;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WanderingRefugeeClientHelper {
    public static void openScreen(WanderingRefugee entity) {
        Minecraft.getInstance().setScreen(new WanderingRefugeeScreen(entity));
    }
}
