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

import java.util.ArrayList;
import java.util.List;

public class CrucibleScreen extends IEContainerScreen<CrucibleContainer> {
    private static final ResourceLocation TEXTURE = FHScreenUtils.makeTextureLocation("generatornew");
    private CrucibleTile tile;

    public CrucibleScreen(CrucibleContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
    }

    @Override
    public void init() {
        super.init();

    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();

    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);

    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= guiLeft + x && mouseY >= guiTop + y
                && mouseX < guiLeft + x + w && mouseY < guiTop + y + h;
    }
}
