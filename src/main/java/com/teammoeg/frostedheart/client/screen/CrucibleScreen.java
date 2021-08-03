package com.teammoeg.frostedheart.client.screen;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.common.container.CrucibleContainer;
import com.teammoeg.frostedheart.common.tile.CrucibleTile;
import com.teammoeg.frostedheart.util.FHScreenUtils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class CrucibleScreen extends IEContainerScreen<CrucibleContainer> {
    private static final ResourceLocation TEXTURE = FHScreenUtils.makeTextureLocation("crucible");
    private CrucibleTile tile;

    public CrucibleScreen(CrucibleContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
        clearIntArray(tile.guiData);
    }

    @Override
    public void init() {
      super.init();
    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);
        if (tile.temperature > 0) {
            int temp = tile.temperature;
            int bar = temp / 30;
            this.blit(transform, guiLeft + 12, guiTop + 67 - bar, 177, 83 - bar, 5, bar);
        }
        if (tile.burnTime > 0) {
            int h = tile.burnTime / 46;
            this.blit(transform, guiLeft + 84, guiTop + 47 - h, 179, 1 + 12 - h, 9, h);
        }
        if (tile.process > 0) {
            int h = tile.process / tile.processMax * 21;
            this.blit(transform, guiLeft + 78, guiTop + 16, 204, 15, 21 - h, 15);
        }

    }


}
