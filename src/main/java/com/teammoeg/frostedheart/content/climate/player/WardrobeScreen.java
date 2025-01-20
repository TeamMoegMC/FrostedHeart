package com.teammoeg.frostedheart.content.climate.player;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WardrobeScreen extends IEContainerScreen<WardrobeContainer> {
    private static final ResourceLocation TEXTURE = Lang.makeTextureLocation("wardrobe");

    public WardrobeScreen(WardrobeContainer inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title,TEXTURE);
    }
}
