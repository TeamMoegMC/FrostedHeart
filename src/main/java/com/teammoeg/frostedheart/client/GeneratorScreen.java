package com.teammoeg.frostedheart.client;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.stereowalker.survive.Survive;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.common.GeneratorContainer;
import com.teammoeg.frostedheart.common.GeneratorTileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class GeneratorScreen extends IEContainerScreen<GeneratorContainer> {
//    private static final ResourceLocation TEXTURE = FHScreenUtils.makeTextureLocation("generator");
    private static final ResourceLocation TEXTURE = makeTextureLocation("coke_oven");
    private GeneratorTileEntity tile;

    public GeneratorScreen(GeneratorContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
        clearIntArray(tile.guiData);
    }

    @Override
    public void render(MatrixStack transform, int mx, int my, float partial) {
        super.render(transform, mx, my, partial);
        font.drawString(transform, "Temperature Level: " + tile.temperatureLevel, guiLeft + 10, guiTop + 10, 0);
        font.drawString(transform, "Range Level: " + tile.rangeLevel, guiLeft + 10, guiTop + 20, 0);
        font.drawString(transform, "Temperature Modifier: " + Survive.blockTemperatureMap.get(FHMain.rl("generator")).getTemperatureModifier(), guiLeft + 10, guiTop + 30, 0);
        font.drawString(transform, "Effective Range: " + Survive.blockTemperatureMap.get(FHMain.rl("generator")).getRange(), guiLeft + 10, guiTop + 40, 0);

        List<ITextComponent> tooltip = new ArrayList<>();
        if(!tooltip.isEmpty())
            GuiUtils.drawHoveringText(transform, tooltip, mx, my, width, height, -1, font);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);

        if(tile.processMax > 0&&tile.process > 0)
        {
            int h = (int)(12*(tile.process/(float)tile.processMax));
            this.blit(transform, guiLeft+59, guiTop+37+12-h, 179, 1+12-h, 9, h);
        }
    }

}
