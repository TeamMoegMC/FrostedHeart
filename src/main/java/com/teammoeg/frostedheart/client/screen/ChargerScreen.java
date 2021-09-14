package com.teammoeg.frostedheart.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.container.ChargerContainer;
import com.teammoeg.frostedheart.container.GeneratorContainer;
import com.teammoeg.frostedheart.tileentity.BurnerGeneratorTileEntity;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class ChargerScreen extends IEContainerScreen<ChargerContainer>{
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("generatornew");

    public ChargerScreen(ChargerContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
    }
    @Override
    public void init() {
        super.init();
        this.buttons.clear();
    }
    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();

        if (isMouseIn(mouseX, mouseY, 12, 13, 2, 54)) {
            tooltip.add(GuiUtils.translateGui("charger.power").appendString(Float.toString(this.getContainer().tile.power)));
        }
        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }
    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);
        float tempLevel=this.getContainer().tile.power/500000*4;
        int offset = (int) ((4 - tempLevel) * 14);
        int bar = (int) ((tempLevel - 1) * 14);
        this.blit(transform, guiLeft + 12, guiTop + 13 + offset, 181, 30, 2, 12 + bar);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= guiLeft + x && mouseY >= guiTop + y
                && mouseX < guiLeft + x + w && mouseY < guiTop + y + h;
    }
}
