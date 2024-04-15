package com.teammoeg.frostedheart.content.tips.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.UnlockedTipManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.TipDisplayUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class DebugScreen extends Screen {
    public DebugScreen() {
        super(new StringTextComponent(""));
    }

    @Override
    public void init() {
        this.addButton(new IconButton((int) (this.width*0.5-25), (int) (this.height*0.4), IconButton.ICON_TRASH_CAN, 0xFFC6FCFF, new TranslationTextComponent(FHMain.MODID + ".tips.gui.clear_cache"), (b) -> {
            TipDisplayUtil.clearCache();
        }));
        this.addButton(new IconButton((int) (this.width*0.5-5), (int) (this.height*0.4), IconButton.ICON_CROSS, 0xFFC6FCFF, new TranslationTextComponent(FHMain.MODID + ".tips.gui.clear_queue"), (b) -> {
            TipDisplayUtil.clearRenderQueue();
        }));
        this.addButton(new IconButton((int) (this.width*0.5+15), (int) (this.height*0.4), IconButton.ICON_HISTORY, 0xFFFF5340, new TranslationTextComponent(FHMain.MODID + ".tips.gui.reset_unlock"), (b) -> {
            UnlockedTipManager.manager.createFile();
        }));
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        fill(ms, (int) (this.width*0.5-30), (int) (this.height*0.4-5), (int) (this.width*0.5+30), (int) (this.height*0.4+15), 0x80000000);
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
