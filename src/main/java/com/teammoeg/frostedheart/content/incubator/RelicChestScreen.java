package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class RelicChestScreen extends IEContainerScreen<RelicChestContainer> {
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("relic_chest");
    public RelicChestScreen(RelicChestContainer inventorySlotsIn, PlayerInventory inv, ITextComponent title) {
        super(inventorySlotsIn, inv, title);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
