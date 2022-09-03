package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.client.util.Point;
import com.teammoeg.frostedheart.client.util.UV;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class SaunaScreen extends IEContainerScreen<SaunaContainer> {
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("sauna_vent");

    private static final UV clock1 = new UV(176, 0, 38, 38);
    private static final UV clock2 = new UV(214, 0, 38, 38);
    private static final UV clock3 = new UV(176, 38, 38, 38);
    private static final UV clock4 = new UV(214, 38, 38, 38);

    private static final UV flame1 = new UV(176, 76, 16, 29);
    private static final UV flame2 = new UV(192, 76, 16, 29);
    private static final UV flame3 = new UV(208, 76, 16, 29);
    private static final UV flame4 = new UV(224, 76, 16, 29);
    private static final UV flame5 = new UV(240, 76, 16, 29);

    private static final Point clockPos = new Point(41, 15);
    private static final Point flamePos = new Point(122, 19);

    public SaunaScreen(SaunaContainer inventorySlotsIn, PlayerInventory inv, ITextComponent title) {
        super(inventorySlotsIn, inv, title);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        ClientUtils.bindTexture(TEXTURE);
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);

        SaunaTileEntity tile = this.container.tile;

        // draw the steam clock
        float powerFraction = tile.getPowerFraction();

        if (powerFraction < 0.25) {
            clock1.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        }
        else if (powerFraction < 0.5) {
            clock2.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        }
        else if (powerFraction < 0.75) {
            clock3.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        }
        else {
            clock4.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        }

        // draw flame if the sauna is on
        if (tile.isWorking()) {
            flame1.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, flamePos);
            Item medicine = container.getSlot(0).getStack().getItem();
            if (container.getSlot(0).getHasStack()) {
                // TODO: add more medicine and change to recipe system
                if (medicine == Items.WHEAT) {
                    flame2.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, flamePos);
                }
                else if (medicine == Items.LAPIS_LAZULI) {
                    flame3.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, flamePos);
                }
                else if (medicine == Items.SUGAR_CANE) {
                    flame4.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, flamePos);
                }
                else if (medicine == Items.CHARCOAL) {
                    flame5.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, flamePos);
                }
            }
        }

    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
        super.drawGuiContainerForegroundLayer(matrixStack, x, y);
    }
}
