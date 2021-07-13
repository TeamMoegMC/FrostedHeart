package com.teammoeg.frostedheart.client.screen;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonBoolean;
import blusunrize.immersiveengineering.common.network.MessageTileSync;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.stereowalker.survive.Survive;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.common.tile.GeneratorTileEntity;
import com.teammoeg.frostedheart.common.container.GeneratorContainer;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.util.FHScreenUtils;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

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
        this.buttons.clear();
        this.addButton(new GuiButtonBoolean(guiLeft+56, guiTop+35, 19, 10, "", tile.isWorking(), TEXTURE, 0, 245, 0,
                btn -> {
                    CompoundNBT tag = new CompoundNBT();
                    tile.setWorking(!btn.getState());
                    tag.putBoolean("isWorking", tile.isWorking());
                    PacketHandler.get().sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
        this.addButton(new GuiButtonBoolean(guiLeft+101, guiTop+35, 19, 10, "", tile.isOverdrive(), TEXTURE, 0, 245, 0,
                btn -> {
                    CompoundNBT tag = new CompoundNBT();
                    tile.setOverdrive(!btn.getState());
                    tag.putBoolean("isOverdrive", tile.isOverdrive());
                    PacketHandler.get().sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();

        if (isMouseIn(mouseX, mouseY, 57,36, 19, 10)) {
            if (tile.isWorking()) {
                tooltip.add(FHScreenUtils.translateGui("generator.mode.off"));
            } else {
                tooltip.add(FHScreenUtils.translateGui("generator.mode.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 102,36, 19, 10)) {
            if (tile.isOverdrive()) {
                tooltip.add(FHScreenUtils.translateGui("generator.overdrive.off"));
            } else {
                tooltip.add(FHScreenUtils.translateGui("generator.overdrive.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 12,13, 2, 54) && tile.getIsActive()) {
            tooltip.add(FHScreenUtils.translateGui("generator.temperature.level").appendString(Integer.toString(tile.getActualTemp())));
        }

        if (isMouseIn(mouseX, mouseY, 161,13, 2, 54) && tile.getIsActive()) {
            tooltip.add(FHScreenUtils.translateGui("generator.range.level").appendString(Integer.toString(tile.getActualRange())));
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
            this.blit(transform, guiLeft + 84, guiTop + 47 - h, 179, 1 + 12 - h, 9, h);
        }

        // work button
        if (tile.isWorking()) {
            this.blit(transform, guiLeft + 56, guiTop + 35, 232, 1, 19, 10);
        }

        // overdrive button
        if (tile.isOverdrive()) {
            this.blit(transform, guiLeft + 101, guiTop + 35, 232, 12, 19, 10);
        }

        int tempLevel = tile.temperatureLevel;
        int rangeLevel = tile.rangeLevel;

        // temperature bar (182, 30)
        if (tile.getIsActive()) {
            int offset = (4 - tempLevel) * 14;
            int bar = (tempLevel - 1) * 14;
            this.blit(transform, guiLeft + 12, guiTop + 13 + offset, 181, 30, 2, 12 + bar);
        }

        // range bar
        if (tile.getIsActive()) {
            int offset = (4 - rangeLevel) * 14;
            int bar = (rangeLevel - 1) * 14;
            this.blit(transform, guiLeft + 161, guiTop + 13 + offset, 181, 30, 2, 12 + bar);
        }
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h)
    {
        return mouseX >= guiLeft+x&&mouseY >= guiTop+y
                &&mouseX < guiLeft+x+w&&mouseY < guiTop+y+h;
    }
}
