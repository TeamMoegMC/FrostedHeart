package com.teammoeg.frostedheart.content.tips.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.UnlockedTipManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.TipDisplayUtil;
import com.teammoeg.frostedheart.content.tips.client.waypoint.WaypointManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import com.teammoeg.frostedheart.util.TranslateUtils;

import java.util.Random;
import java.util.UUID;

public class DebugScreen extends Screen {
    public DebugScreen() {
        super(new StringTextComponent(""));
    }

    @Override
    public void init() {
        this.addButton(new IconButton((int) (this.width*0.5-45), (int) (this.height*0.4), IconButton.ICON_TRASH_CAN, 0xFFC6FCFF, TranslateUtils.translate(FHMain.MODID + ".tips.gui.clear_cache"), (b) -> {
            TipDisplayUtil.clearCache();
        }));
        this.addButton(new IconButton((int) (this.width*0.5-25), (int) (this.height*0.4), IconButton.ICON_CROSS, 0xFFC6FCFF, TranslateUtils.translate(FHMain.MODID + ".tips.gui.clear_queue"), (b) -> {
            TipDisplayUtil.clearRenderQueue();
        }));
        this.addButton(new IconButton((int) (this.width*0.5-5), (int) (this.height*0.4), IconButton.ICON_HISTORY, 0xFFFF5340, TranslateUtils.translate(FHMain.MODID + ".tips.gui.reset_unlock"), (b) -> {
            UnlockedTipManager.manager.createFile();
        }));
        this.addButton(new IconButton((int) (this.width*0.5+15), (int) (this.height*0.4), IconButton.ICON_BOX_ON, 0xFFC6FCFF, new StringTextComponent("Add a random waypoint"), (b) -> {
            Random random = new Random();
            String uuid = UUID.randomUUID().toString();
            WaypointManager.create((random.nextFloat()-0.5F)*256, (random.nextFloat()-0.5F)*128+128, (random.nextFloat()-0.5F)*256, uuid, false);
            WaypointManager.setFocus(random.nextFloat() > 0.5F, uuid);
        }));
        this.addButton(new IconButton((int) (this.width*0.5+35), (int) (this.height*0.4), IconButton.ICON_BOX, 0xFFFF5340, new StringTextComponent("Remove all waypoint"), (b) -> {
            WaypointManager.removeAll();
        }));
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        fill(ms, (int) (this.width*0.5-50), (int) (this.height*0.4-5), (int) (this.width*0.5+50), (int) (this.height*0.4+15), 0x80000000);
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
