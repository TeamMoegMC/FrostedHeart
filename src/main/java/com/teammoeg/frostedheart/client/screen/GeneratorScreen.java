package com.teammoeg.frostedheart.client.screen;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.stereowalker.survive.Survive;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.common.tile.GeneratorTileEntity;
import com.teammoeg.frostedheart.common.container.GeneratorContainer;
import com.teammoeg.frostedheart.util.FHScreenUtils;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class GeneratorScreen extends IEContainerScreen<GeneratorContainer> {
    private static final ResourceLocation TEXTURE = FHScreenUtils.makeTextureLocation("generatornew");
    private GeneratorTileEntity tile;

    public GeneratorScreen(GeneratorContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
        clearIntArray(tile.guiData);
    }

    @Override
    public void init() {
        super.init();
        this.addButton(new ImageButton(guiLeft+56, guiTop+35, 19, 10, 232, 1, 12, TEXTURE,
                btn -> {
                    tile.setWorking(!tile.isWorking());
                    fullInit();
                }));
    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        int tempLevel = tile.temperatureLevel;
        int rangeLevel = tile.rangeLevel;
        float tempMod = Survive.blockTemperatureMap.get(FHMain.rl("generator")).getTemperatureModifier();
        int range = Survive.blockTemperatureMap.get(FHMain.rl("generator")).getRange();

        List<ITextComponent> tooltip = new ArrayList<>();

        if (isMouseIn(mouseX, mouseY, 57,36, 19, 10)) {
            if (tile.isWorking()) {
                tooltip.add(new StringTextComponent("Turn Off Generator"));
            } else {
                tooltip.add(new StringTextComponent("Turn On Generator"));
            }

        }

        if (isMouseIn(mouseX, mouseY, 102,36, 19, 10)) {
            if (tile.isOverdrive()) {
                tooltip.add(new StringTextComponent("Turn Off Overdrive"));
            } else {
                tooltip.add(new StringTextComponent("Turn On Overdrive"));
            }
        }

        if (!tooltip.isEmpty()){
            GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);

        // recipe progress icon
        if (tile.processMax > 0 && tile.process > 0) {
            int h = (int) (12 * (tile.process / (float) tile.processMax));
            this.blit(transform, guiLeft + 85, guiTop + 35 - h, 179, 1 + 12 - h, 9, h);
        }

//        // work button
//        if (tile.isWorking()) {
//            this.blit(transform, guiLeft + 56, guiTop + 35, 232, 1, 19, 10);
//        }
//
//        // overdrive button
//        if (tile.isOverdrive()) {
//            this.blit(transform, guiLeft + 101, guiTop + 35, 232, 12, 19, 10);
//        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // buttons
//        boolean f = isMouseIn((int)mouseX, (int)mouseY, 57,36, 19, 10);
//        if (f) {
//            tile.setWorking(!tile.isWorking());
//            return true;
//        }
//        if (isMouseIn((int)mouseX, (int)mouseY, 102,36, 19, 10)) {
//            tile.setOverdrive(!tile.isOverdrive());
//            return true;
//        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h)
    {
        return mouseX >= guiLeft+x&&mouseY >= guiTop+y
                &&mouseX < guiLeft+x+w&&mouseY < guiTop+y+h;
    }
}
