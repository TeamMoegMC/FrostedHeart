package com.teammoeg.frostedheart.common;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GeneratorScreen extends IEContainerScreen<GeneratorContainer> {
    public GeneratorScreen(GeneratorContainer inventorySlotsIn, PlayerInventory inv, ITextComponent title) {
        super(inventorySlotsIn, inv, title);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {

    }
}
