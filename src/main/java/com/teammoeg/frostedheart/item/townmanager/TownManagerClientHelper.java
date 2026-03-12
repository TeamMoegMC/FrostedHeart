package com.teammoeg.frostedheart.item.townmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TownManagerClientHelper {
    public static void openScreen() {
        Minecraft.getInstance().setScreen(
                new TownManagerScreen(Component.translatable("gui.frostedheart.town_manager"))
        );
    }
}
