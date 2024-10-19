package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import com.teammoeg.frostedheart.content.tips.TipLockManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class DebugScreen extends Screen {
    public DebugScreen() {
        super(TranslateUtils.str(""));
    }

    @Override
    public void init() {
        this.addRenderableWidget(new IconButton((int) (this.width*0.5-45), (int) (this.height*0.4), IconButton.ICON_TRASH_CAN, 0xFFC6FCFF, TranslateUtils.translate(FHMain.MODID + ".tips.gui.clear_cache"), (b) -> {
            TipDisplayManager.clearCache();
        }));
        this.addRenderableWidget(new IconButton((int) (this.width*0.5-25), (int) (this.height*0.4), IconButton.ICON_CROSS, 0xFFC6FCFF, TranslateUtils.translate(FHMain.MODID + ".tips.gui.clear_queue"), (b) -> {
            TipDisplayManager.clearRenderQueue();
        }));
        this.addRenderableWidget(new IconButton((int) (this.width*0.5-5), (int) (this.height*0.4), IconButton.ICON_HISTORY, 0xFFFF5340, TranslateUtils.translate(FHMain.MODID + ".tips.gui.reset_unlock"), (b) -> {
            TipLockManager.manager.createFile();
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill( (int) (this.width*0.5-50), (int) (this.height*0.4-5), (int) (this.width*0.5+50), (int) (this.height*0.4+15), 0x80000000);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
